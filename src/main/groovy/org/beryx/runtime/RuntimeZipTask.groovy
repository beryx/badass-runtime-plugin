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
import org.beryx.runtime.data.RuntimePluginExtension
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.RuntimeZipTaskImpl
import org.beryx.runtime.data.RuntimeZipTaskData
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class RuntimeZipTask extends BaseTask {
    @Input
    Property<Map<String, TargetPlatform>> targetPlatforms

    @InputDirectory
    DirectoryProperty imageDir

    @OutputFile
    RegularFileProperty imageZip

    RuntimeZipTask() {
        dependsOn(RuntimePlugin.TASK_NAME_RUNTIME)
        description = 'Creates a zip of the runtime image of your application'
    }

    @Override
    void init(RuntimePluginExtension extension) {
        super.init(extension)
        targetPlatforms = extension.targetPlatforms
        imageDir = extension.imageDir
        imageZip = extension.imageZip
    }

    @TaskAction
    void runtimeZipTaskAction() {
        def taskData = new RuntimeZipTaskData()
        taskData.targetPlatforms = targetPlatforms.get()
        taskData.imageDir = imageDir.get().asFile
        taskData.imageZip = imageZip.get().asFile
        def taskImpl = new RuntimeZipTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
