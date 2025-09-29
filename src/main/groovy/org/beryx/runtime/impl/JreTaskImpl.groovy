/*
 * Copyright 2019 the original author or authors.
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
package org.beryx.runtime.impl

import groovy.transform.CompileStatic
import org.beryx.runtime.data.JreTaskData
import org.beryx.runtime.util.SuggestedModulesBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import javax.inject.Inject

abstract class JreTaskImpl extends BaseTaskImpl<JreTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JreTaskImpl)

    @Inject
    JreTaskImpl(Project project, JreTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileStatic
    void execute() {
        if(td.targetPlatforms) {
            td.targetPlatforms.values().each { platform ->
                File jreDir = new File(td.jreDir, "$project.name-$platform.name")
                createJre(jreDir, platform.jdkHome.getOrNull(), td.options + platform.options.get())
            }
        } else {
            createJre(td.jreDir, td.javaHome, td.options)
        }
    }

    void createJre(File jreDir, String jdkHome, List<String> options) {
        project.delete(jreDir)

        def cmd = ["$td.javaHome/bin/jlink",
                       '-v',
                       *options,
                       '--add-modules', modules.join(','),
                       '--output', jreDir]

        if (project.file("$jdkHome/jmods").directory) {
            cmd += ['--module-path',
                    "$jdkHome/jmods/"]
        }

        LOGGER.info("Executing: $cmd")
        def result = exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jlinkOutput = {
                return standardOutput.toString()
            }
            commandLine = cmd
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jlinkOutput())
        } else {
            LOGGER.info(project.ext.jlinkOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    @CompileStatic
    Collection<String> getModules() {
        Set<String> imageModules = []
        if(td.additive || !td.modules) {
            imageModules.addAll(new SuggestedModulesBuilder(td.javaHome).getProjectModules(project))
        }
        if(td.modules) {
            imageModules.addAll(td.modules)
        }
        imageModules
    }
}
