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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.JPackageData
import org.beryx.runtime.data.JPackageTaskData
import org.beryx.runtime.impl.JPackageImageTaskImpl
import org.gradle.api.file.Directory
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction

@CompileStatic
abstract class JPackageImageTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger(JPackageImageTask.class)

    @InputDirectory
	@Optional
    abstract DirectoryProperty getDistDir()

    @InputDirectory
    abstract DirectoryProperty getJreDir()

    @Nested
    abstract Property<JPackageData> getJpackageData()

    @Input
    @Optional
    abstract Property<String> getJavaHome()

    @Input
    abstract Property<String> getDefaultJavaHome()

    @Internal
    Sync getDistTask() {
        (Sync)(project.tasks.findByName('installShadowDist') ?: project.tasks.getByName('installDist'))
    }

    /*@CompileDynamic
    JPackageImageTask() {
        description = 'Creates an application image using the jpackage tool'
        dependsOn(RuntimePlugin.TASK_NAME_JRE)
        project.afterEvaluate {
            dependsOn(distTask)
        }
    }*/

	private Directory getDistDirRuntime(){
		return distDir.getOrNull() ?: project.layout.buildDirectory.dir(distTask.destinationDir.path).get()
	}

    private String getJavaHomeOrDefault() {
        return javaHome.present ? javaHome.get() : defaultJavaHome.get()
    }

    @TaskAction
    void jpackageTaskAction() {
        def taskData = new JPackageTaskData()
        taskData.distDir = distDirRuntime.asFile
        taskData.jpackageData = jpackageData.get()
        taskData.javaHome = javaHomeOrDefault

        def jreTask = (JreTask) project.tasks.getByName(RuntimePlugin.TASK_NAME_JRE)
        taskData.configureRuntimeImageDir(jreTask)

        def taskImpl = new JPackageImageTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
