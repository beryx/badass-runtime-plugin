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
package org.beryx.runtime


import groovy.transform.CompileStatic
import org.beryx.runtime.data.JreTaskData
import org.beryx.runtime.data.RuntimePluginExtension
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.JreTaskImpl
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class JreTask extends BaseTask {
    @Input
    ListProperty<String> options

    @Input
    ListProperty<String> modules

    @Input
    Property<String> javaHome

    @Input
    Provider<Map<String, TargetPlatform>> targetPlatforms

    @OutputDirectory
    DirectoryProperty jreDir

    JreTask() {
        description = 'Creates a custom JRE'
    }

    @Override
    void init(RuntimePluginExtension extension) {
        super.init(extension)
        options = extension.options
        modules = extension.modules
        javaHome = extension.javaHome
        targetPlatforms = extension.targetPlatforms
        jreDir = extension.jreDir
    }

    @TaskAction
    void runtimeTaskAction() {
        def taskData = new JreTaskData()
        taskData.jreDir = jreDir.get().asFile
        taskData.options = options.get()
        taskData.modules = modules.get()
        taskData.javaHome = javaHome.get()
        taskData.targetPlatforms = targetPlatforms.get()

        def taskImpl = new JreTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
