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
import org.gradle.api.provider.Provider

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.CdsData
import org.beryx.runtime.data.LauncherData
import org.beryx.runtime.data.RuntimeTaskData
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.impl.RuntimeTaskImpl
import org.beryx.runtime.util.Util
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.Directory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.jvm.application.scripts.ScriptGenerator
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator

@CompileStatic
abstract class RuntimeTask extends DefaultTask {
    @Nested
    abstract MapProperty<String, TargetPlatform> getTargetPlatforms()

    @Nested
    abstract Property<LauncherData> getLauncherData()

    @Nested
    abstract Property<CdsData> getCdsData()

    @InputDirectory
    @Optional
    abstract DirectoryProperty getDistDir()

    @OutputDirectory
    abstract DirectoryProperty getJreDir()

    @Internal
    abstract DirectoryProperty getImageDir()

    @Internal
    Sync getDistTask() {
        (Sync)(project.tasks.findByName('installShadowDist') ?: project.tasks.getByName('installDist'))
    }

    /*@CompileDynamic
    RuntimeTask() {
        description = 'Creates a runtime image of your application'
        dependsOn(RuntimePlugin.TASK_NAME_JRE)
        project.afterEvaluate {
            dependsOn(distTask)
        }
        project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
            configureStartScripts(taskGraph.hasTask(this))
        }
    }*/

    @OutputDirectory
    Provider<File> getImageDirAsFile() {
        imageDir.asFile
    }

    void configureStartScripts(boolean asRuntimeImage) {
        project.tasks.withType(CreateStartScripts) { CreateStartScripts startScriptTask ->
            startScriptTask.mainClass.set(Util.getMainClass(project));
            startScriptTask.defaultJvmOpts = launcherData.get().jvmArgsOrDefault
            startScriptTask.doLast {
                startScriptTask.unixScript.text = startScriptTask.unixScript.text.replace('{{BIN_DIR}}', '$APP_HOME/bin')
                startScriptTask.unixScript.text = startScriptTask.unixScript.text.replace('{{HOME_DIR}}', '$HOME')
                startScriptTask.unixScript.text = startScriptTask.unixScript.text.replaceAll(/\{\{([\w.]+)}}/, '\\$$1')

                startScriptTask.windowsScript.text = startScriptTask.windowsScript.text.replace('{{BIN_DIR}}', '%APP_HOME%\\\\bin')
                startScriptTask.windowsScript.text = startScriptTask.windowsScript.text.replace('{{HOME_DIR}}', '%USERPROFILE%')
                startScriptTask.windowsScript.text = startScriptTask.windowsScript.text.replaceAll(/\{\{([\w.]+)}}/, '%$1%')
            }
            startScriptTask.inputs.property('asRuntimeImage', asRuntimeImage)
            if(asRuntimeImage) {
                if(launcherData.get().runInBinDir) {
                    System.properties['BADASS_RUN_IN_BIN_DIR'] = 'true'
                }
                configureCds()
                configureTemplate(startScriptTask.unixStartScriptGenerator, launcherData.get().unixTemplateUrl)
                configureTemplate(startScriptTask.windowsStartScriptGenerator, launcherData.get().windowsTemplateUrl)
            }
        }
    }

    @CompileDynamic
    private void configureCds() {
        if (cdsData.get().enabled) {
            this.doLast {
                project.exec {
                    commandLine = ["$imageDir.get()/bin/java", "-Xshare:dump"]
                }
            }
            System.properties['BADASS_CDS_ARCHIVE_FILE_LINUX'] = cdsData.get().sharedArchiveFile ?: '$APP_HOME/lib/server/$APP_NAME.jsa'
            System.properties['BADASS_CDS_ARCHIVE_FILE_WINDOWS'] = cdsData.get().sharedArchiveFile ?: '%~dp0\\server\\%~n0.jsa'
        }
    }

    void configureTemplate(ScriptGenerator scriptGenerator, URL template) {
        ((TemplateBasedScriptGenerator)scriptGenerator).template = project.resources.text.fromString(template.text)
    }

    private Directory getDistDirRuntime(){
        return distDir.getOrNull() ?: project.layout.buildDirectory.dir(distTask.destinationDir.path).get()
    }

    @TaskAction
    void runtimeTaskAction() {
        def taskData = new RuntimeTaskData()
        taskData.distDir = distDirRuntime.asFile
        taskData.jreDir = jreDir.asFile.get()
        taskData.imageDir = imageDir.asFile.get()
        taskData.targetPlatforms = targetPlatforms.get()

        def taskImpl = new RuntimeTaskImpl(project, taskData)
        taskImpl.execute()
    }
}
