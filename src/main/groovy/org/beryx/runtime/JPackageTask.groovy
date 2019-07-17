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

import groovy.transform.CompileStatic
import org.beryx.runtime.data.JPackageData
import org.beryx.runtime.data.JPackageTaskData
import org.beryx.runtime.impl.JPackageTaskImpl
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

@CompileStatic
class JPackageTask extends BaseTask {
    private static final Logger LOGGER = Logging.getLogger(JPackageTask.class)

    @InputDirectory
    Directory getJreDir() {
        extension.jreDir.get()
    }

    @InputDirectory
    Directory getImageDir() {
        extension.imageDir.get()
    }

    @Nested
    JPackageData getJpackageData() {
        extension.jpackageData.get()
    }

    JPackageTask() {
        dependsOn(RuntimePlugin.TASK_NAME_JPACKAGE_IMAGE)
        description = 'Creates an application installer using the jpackage tool'
    }

    @TaskAction
    void jpackageTaskAction() {
        def taskData = new JPackageTaskData()
        taskData.imageDir = imageDir.asFile
        taskData.jpackageData = jpackageData
        taskData.mainClass = defaultMainClass

        def runtimeTask = (RuntimeTask) project.tasks.getByName(RuntimePlugin.TASK_NAME_RUNTIME)
        taskData.configureRuntimeImageDir(runtimeTask)

        def taskImpl = new JPackageTaskImpl(project, taskData)
        taskImpl.execute()
    }

    @Internal
    String getDefaultMainClass() {
        def mainClass = project['mainClassName'] as String
        int pos = mainClass.lastIndexOf('/')
        if(pos < 0) return mainClass
//        def mainClassModule = mainClass.substring(0, pos)
        mainClass.substring(pos + 1)
    }
}
