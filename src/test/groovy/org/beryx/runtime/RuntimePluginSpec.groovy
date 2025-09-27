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
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll
import spock.util.environment.OperatingSystem

import java.nio.file.Path

import static org.gradle.testkit.runner.TaskOutcome.*

class RuntimePluginSpec extends Specification {
    @TempDir Path testProjectDir

    def cleanup() {
        println "CLEANUP"
    }

    def setUpBuild(Collection<String> modules) {
        new AntBuilder().copy( todir: testProjectDir ) {
            fileset( dir: 'src/test/resources/hello-logback' )
        }

        File buildFile = new File(testProjectDir.toFile(), "build.gradle")
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
    def "if modules=#modules, then buildSucceeds=#buildShouldSucceed and runSucceeds=#runShouldSucceed with Gradle #gradleVersion"() {
        when:
        setUpBuild(modules)
        BuildResult result
        try {
            result = GradleRunner.create()
                    .withDebug(true)
                    .withProjectDir(testProjectDir.toFile())
                    .withGradleVersion(gradleVersion)
                    .withPluginClasspath()
                    .withArguments(RuntimePlugin.TASK_NAME_RUNTIME, "-is")
                    .build()
        } catch (Exception e) {
            if(buildShouldSucceed) {
                e.printStackTrace()
            }
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
        null                                        | true               | true             | '7.6'
        []                                          | true               | true             | '7.6'
        ['java.base']                               | true               | false            | '7.4'
        ['foo.bar']                                 | false              | false            | '7.6'
        ['java.naming']                             | true               | false            | '7.4'
        ['java.naming', 'java.xml']                 | true               | true             | '7.6'
        ['java.naming', 'java.xml', 'java.logging'] | true               | true             | '7.4'
        ['java.naming', 'java.xml', 'foo.bar']      | false              | false            | '7.6'
    }
}
