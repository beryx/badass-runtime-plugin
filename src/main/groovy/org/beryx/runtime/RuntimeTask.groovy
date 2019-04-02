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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.RuntimePluginExtension
import org.beryx.runtime.data.RuntimeTaskData
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.RuntimeTaskImpl
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.application.CreateStartScripts

@CompileStatic
class RuntimeTask extends BaseTask {
    @Input
    ListProperty<String> options

    @Input
    ListProperty<String> modules

    @Input
    Property<String> javaHome

    @Input
    Provider<Map<String, TargetPlatform>> targetPlatforms

    @InputDirectory
    DirectoryProperty distDir

    @OutputDirectory
    DirectoryProperty imageDir

    RuntimeTask() {
        description = 'Creates a runtime image of your application'
        project.afterEvaluate {
            if(!distDir.getOrNull()) {
                Sync distTask = (Sync)(project.tasks.findByName('installShadowDist') ?: project.tasks.getByName('installDist'))
                dependsOn(distTask)
                distDir.set(distTask.destinationDir)
            }
            configureStartScripts(project)
        }
    }

    @CompileDynamic
    static void configureStartScripts(Project project) {
        project.tasks.withType(CreateStartScripts) {
            unixStartScriptGenerator.template = getTextResource(project, '/unixScriptTemplate.txt')
            windowsStartScriptGenerator.template = getTextResource(project, '/windowsScriptTemplate.txt')
        }
    }

    static TextResource getTextResource(Project project, String path) {
        def template = RuntimePlugin.class.getResource(path)
        if(!template) throw new GradleException("Resource $path not found.")
        project.resources.text.fromString(template.text)
    }

    @Override
    void init(RuntimePluginExtension extension) {
        super.init(extension)
        options = extension.options
        modules = extension.modules
        javaHome = extension.javaHome
        targetPlatforms = extension.targetPlatforms
        distDir = extension.distDir
        imageDir = extension.imageDir
    }

    @TaskAction
    void runtimeTaskAction() {
        def taskData = new RuntimeTaskData()
        taskData.distDir = distDir.get().asFile
        taskData.imageDir = imageDir.get().asFile
        taskData.options = options.get()
        taskData.modules = modules.get()
        taskData.javaHome = javaHome.get()
        taskData.targetPlatforms = targetPlatforms.get()

        def taskImpl = new RuntimeTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
