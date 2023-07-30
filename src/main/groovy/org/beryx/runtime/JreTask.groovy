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
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.JreTaskImpl
import org.gradle.api.file.Directory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
class JreTask extends BaseTask {
    @Input
    List<String> getOptions() {
        extension.options.get()
    }

    @Input
    boolean isAdditive() {
        extension.additive.get()
    }

    @Input
    List<String> getModules() {
        extension.modules.get()
    }

    @Input
    String getJavaHome() {
        javaHomeOrDefault
    }

    @Nested
    MapProperty<String, TargetPlatform> getTargetPlatforms() {
        extension.targetPlatforms
    }

    @OutputDirectory
    Directory getJreDir() {
        extension.jreDir.get()
    }

    @OutputDirectory
    File getJreDirAsFile() {
        jreDir.asFile
    }

    JreTask() {
        dependsOn('jar')
        description = 'Creates a custom java runtime image with jlink'
        dependsOn(project.configurations['runtimeClasspath'].allDependencies)
    }

    @TaskAction
    void runtimeTaskAction() {
        def taskData = new JreTaskData()
        taskData.jreDir = jreDir.asFile
        taskData.options = options
        taskData.additive = additive
        taskData.modules = modules
        taskData.javaHome = javaHome
        taskData.targetPlatforms = targetPlatforms.get()

        def taskImpl = new JreTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
