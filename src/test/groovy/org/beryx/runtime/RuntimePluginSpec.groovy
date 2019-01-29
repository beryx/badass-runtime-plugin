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
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.OperatingSystem

import static org.gradle.testkit.runner.TaskOutcome.*

class RuntimePluginSpec extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    def cleanup() {
        println "CLEANUP"
    }

    def setUpBuild(Collection<String> modules) {
        new AntBuilder().copy( todir: testProjectDir.root ) {
            fileset( dir: 'src/test/resources/hello-logback' )
        }

        File buildFile = new File(testProjectDir.root, "build.gradle")
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
                    .withProjectDir(testProjectDir.root)
                    .withGradleVersion(gradleVersion)
                    .withPluginClasspath()
                    .withArguments(RuntimePlugin.TASK_NAME_RUNTIME, "-is")
                    .build();
        } catch (Exception e) {
            assert !buildShouldSucceed
            return
        }
        def imageBinDir = new File(testProjectDir.root, 'build/image/bin')
        def launcherExt = OperatingSystem.current.windows ? '.bat' : ''
        def imageLauncher = new File(imageBinDir, "runtime-hello$launcherExt")

        then:
        result.task(":$RuntimePlugin.TASK_NAME_RUNTIME").outcome == SUCCESS
        imageLauncher.exists()

        when:
        imageLauncher.setExecutable(true)
        def process = imageLauncher.absolutePath.execute([], imageBinDir)
        def out = new ByteArrayOutputStream(2048)
        process.waitForProcessOutput(out, out)
        def outputText = out.toString()

        then:
        (outputText.trim() == 'LOG: Hello, runtime!') == runShouldSucceed

        where:
        modules                                     | buildShouldSucceed | runShouldSucceed | gradleVersion
        null                                        | true               | true             | '4.8'
        []                                          | true               | true             | '5.0'
        ['java.base']                               | true               | false            | '5.1'
        ['foo.bar']                                 | false              | false            | '5.1.1'
        ['java.naming']                             | true               | false            | '4.8'
        ['java.naming', 'java.xml']                 | true               | true             | '5.0'
        ['java.naming', 'java.xml', 'java.logging'] | true               | true             | '5.1'
        ['java.naming', 'java.xml', 'foo.bar']      | false              | false            | '5.1.1'
    }
}
