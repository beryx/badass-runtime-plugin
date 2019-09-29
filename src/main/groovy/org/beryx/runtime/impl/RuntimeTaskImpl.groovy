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
package org.beryx.runtime.impl

import groovy.transform.CompileStatic
import org.beryx.runtime.data.RuntimeTaskData
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class RuntimeTaskImpl extends BaseTaskImpl<RuntimeTaskData> {
    private static final Logger LOGGER = Logging.getLogger(RuntimeTaskImpl)

    RuntimeTaskImpl(Project project, RuntimeTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileStatic
    void execute() {
        if(td.targetPlatforms) {
            td.targetPlatforms.values().each { platform ->
                File jreDir = new File(td.jreDir, "$project.name-$platform.name")
                File imageDir = new File(td.imageDir, "$project.name-$platform.name")
                createRuntime(jreDir, imageDir)
            }
        } else {
            createRuntime(td.jreDir, td.imageDir)
        }
    }

    void createRuntime(File jreDir, File imageDir) {
        project.delete(imageDir)
        copyJre(jreDir, imageDir)
        copyAppTo(imageDir)
    }

    void copyJre(File jreDir, File imageDir) {
        project.copy {
            from jreDir
            into imageDir
        }
    }

    void copyAppTo(File imageDir) {
        project.copy {
            from td.distDir
            into imageDir
        }
    }
}
