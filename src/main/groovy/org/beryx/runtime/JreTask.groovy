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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import groovy.transform.CompileStatic
import org.beryx.runtime.data.JreTaskData
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.JreTaskImpl
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CompileStatic
abstract class JreTask extends DefaultTask {
    @Input
    abstract ListProperty<String> getOptions()

    @Input
    abstract Property<Boolean> getAdditive()

    @Input
    abstract ListProperty<String> getModules()

    @Input
    @Optional
    abstract Property<String> getJavaHome()

    @Nested
    abstract MapProperty<String, TargetPlatform> getTargetPlatforms()

    @OutputDirectory
    abstract DirectoryProperty getJreDir()

    @TaskAction
    void runtimeTaskAction() {
        def taskData = new JreTaskData()
        taskData.jreDir = jreDir.asFile.get()
        taskData.options = options.get()
        taskData.additive = additive.get()
        taskData.modules = modules.get()
        taskData.javaHome = javaHome.get()
        taskData.targetPlatforms = targetPlatforms.get()

        def taskImpl = new JreTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
