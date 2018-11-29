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
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Collectors

class SuggestModulesSpec extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    def cleanup() {
        println "CLEANUP"
    }

    @Unroll
    def "should suggest the correct list of modules"() {
        given:
        new AntBuilder().copy( todir: testProjectDir.root ) {
            fileset( dir: 'src/test/resources/hello-log4j-2.9.0' )
        }
        File buildFile = new File(testProjectDir.root, "build.gradle")
        def outputWriter = new StringWriter(8192)

        when:
        BuildResult result = GradleRunner.create()
                .withDebug(true)
                .forwardStdOutput(outputWriter)
                .withProjectDir(buildFile.parentFile)
                .withPluginClasspath()
                .withArguments("-is", RuntimePlugin.TASK_NAME_SUGGEST_MODULES)
                .build();
        def task = result.task(":$RuntimePlugin.TASK_NAME_SUGGEST_MODULES")
        println outputWriter

        then:
        task.outcome == TaskOutcome.SUCCESS

        when:
        def taskOutput = outputWriter.toString()
        def modules = getModules(taskOutput)

        then:
        modules as Set == [
                'java.sql',
                'java.naming',
                'java.desktop',
                'java.rmi',
                'java.logging',
                'java.compiler',
                'java.scripting',
                'java.xml',
                'java.management'
        ] as Set
    }

    static Set<String> getModules(String taskOutput) {
        String blockStart = 'modules = ['
        String blockEnd = ']'
        int startPos = taskOutput.indexOf(blockStart)
        assert startPos >= 0
        startPos += blockStart.length()
        int endPos = taskOutput.indexOf(blockEnd, startPos)
        assert endPos >= 0
        def content = taskOutput.substring(startPos, endPos)
        content.lines()
                .map{it.trim()}
                .map{it.replaceAll("[',]", "")}
                .filter{!it.empty}
                .collect(Collectors.toList()) as Set

    }
}
