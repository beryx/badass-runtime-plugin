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

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.Property

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.CdsData
import org.beryx.runtime.data.LauncherData
import org.beryx.runtime.data.TargetPlatform
import org.beryx.runtime.util.Util
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.jvm.application.scripts.ScriptGenerator
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator

@CompileStatic
abstract class RuntimeTask extends DefaultTask {

    private final FileOperations fileOperations

    @Input
    String projectName

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

    @OutputDirectory
    abstract DirectoryProperty getImageDir()

    @Inject
    RuntimeTask(FileOperations fileOperations) {
        this.fileOperations = fileOperations
    }

    void configureStartScripts(boolean asRuntimeImage) {
        project.tasks.withType(CreateStartScripts).configureEach { CreateStartScripts startScriptTask ->
            startScriptTask.mainClass.set(Util.getMainClass(project))
            startScriptTask.defaultJvmOpts = launcherData.get().jvmArgs.get()
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

    @TaskAction
    void runtimeTaskAction() {
        def distDir = distDir.get().asFile
        def jreDir = jreDir.asFile.get()
        def imageDir = imageDir.asFile.get()
        def targetPlatforms = targetPlatforms.get()
        if(targetPlatforms) {
            targetPlatforms.values().each { platform ->
                File jreDirectory = new File(jreDir, "$projectName-$platform.name")
                File imageDirectory = new File(imageDir, "$projectName-$platform.name")
                createRuntime(jreDirectory, imageDirectory, distDir)
            }
        } else {
            createRuntime(jreDir, imageDir, distDir)
        }
    }

    void createRuntime(File jreDir, File imageDir, File distDir) {
        fileOperations.delete(imageDir)
        copyJre(jreDir, imageDir)
        copyAppTo(imageDir, distDir)
    }

    void copyJre(File jreDir, File imageDir) {
        fileOperations.copy {
            it.from jreDir
            it.into imageDir
        }
    }

    void copyAppTo(File imageDir, File distDir) {
        fileOperations.copy {
            it.from distDir
            it.into imageDir
        }
    }
}
