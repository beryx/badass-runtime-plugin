/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.beryx.runtime

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion

import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll
import spock.util.environment.OperatingSystem

import java.nio.file.Path

import static org.gradle.testkit.runner.TaskOutcome.*

class RuntimePluginConfigCacheSpec extends Specification {
    @TempDir Path testProjectDir

    def cleanup() {
        println "CLEANUP"
    }

    def setUpBuild(Collection<String> modules, String gradleVersion) {
        new AntBuilder().copy( todir: testProjectDir ) {
            fileset( dir: 'src/test/resources/hello-logback-cache' )
        }

        File buildFile = new File(testProjectDir.toFile(), "build.gradle")
        if( GradleVersion.version('8.0') <= GradleVersion.version( gradleVersion)) {
            buildFile.text = buildFile.text.replace(
                    "id 'com.dua3.gradle.runtime'",
                    "id 'com.dua3.gradle.runtime'\n    id 'com.github.johnrengelman.shadow' version '8.1.1'"
            )
        }
        buildFile << '''
            runtime {
                options = ['--strip-debug', '--compress', '2', '--no-header-files', '--no-man-pages']
        '''.stripIndent()
        if(modules != null) {
            buildFile << "    modules = [${modules.collect{'\'' + it + '\''}.join(', ')}]\n"
        }
        buildFile << '}\n'
        println "Executing build script:\n${buildFile.text}"
    }

    @Unroll
    def "if modules=#modules, then buildSucceeds=#buildShouldSucceed and runSucceeds=#runShouldSucceed and gradleVersion=#gradleVersion"() {
        when:
        setUpBuild(modules, gradleVersion)
        BuildResult result
        try {
            result = GradleRunner.create()
                    .withDebug(true)
                    .withProjectDir(testProjectDir.toFile())
                    .withGradleVersion(gradleVersion)
                    .withPluginClasspath()
                    .withArguments(RuntimePlugin.TASK_NAME_RUNTIME, "-s", "--configuration-cache")
                    .forwardOutput()
                    .build();
        } catch (Exception e) {
            assert !buildShouldSucceed
            return
        }
        def imageBinDir = new File(testProjectDir.toFile(), 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def imageLauncher = new File(imageBinDir, "runtime-hello$launcherExt")

        then:
        result.task(":$RuntimePlugin.TASK_NAME_RUNTIME").outcome == SUCCESS
        imageLauncher.exists()

        when:
        imageLauncher.setExecutable(true)
        def process = imageLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream(2048)
        def err = new ByteArrayOutputStream(2048)
        process.waitForProcessOutput(out, err)
        def outputText = out.toString()

        then:
        (outputText.trim() == 'LOG: Hello, runtime!') == runShouldSucceed

        where:
        modules                                     | buildShouldSucceed | runShouldSucceed | gradleVersion
        null                                        | true               | true             | '7.4'
        null                                        | true               | true             | '8.0'
    }
}
