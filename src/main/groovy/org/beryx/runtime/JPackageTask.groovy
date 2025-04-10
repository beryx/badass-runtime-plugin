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
import org.gradle.api.GradleException
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations

import groovy.transform.CompileDynamic
import org.beryx.runtime.data.JPackageData
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

import groovy.transform.CompileStatic
import org.beryx.runtime.util.Util

import static org.beryx.runtime.util.Util.EXEC_EXTENSION

@CompileStatic
abstract class JPackageTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger(JPackageTask);

    private final FileOperations fileOperations
    private final ExecOperations execOperations

    @Nested
    abstract Property<JPackageData> getJpackageData()

    @Input
    @Optional
    abstract Property<String> getJavaHome()

    @Inject
    JPackageTask(FileOperations fileOperations, ExecOperations execOperations) {
        this.fileOperations = fileOperations
        this.execOperations = execOperations
    }

    @TaskAction
    @CompileDynamic
    void jpackageTaskAction() {
        def jpackageData = jpackageData.get()
        def javaHome = javaHome.get()

        final def imgOutDir = jpackageData.imageOutputDir.get()
        final def imageName = jpackageData.imageName.get()
        final def appImagePath = "${imgOutDir}${File.separator}${imageName}"
        def appImageDir = new File(appImagePath)
        if ( OperatingSystem.current().macOsX) {
            if (!appImageDir.directory) {
                def currImagePath = "${appImagePath}.app"
                if (!new File(currImagePath).directory) {
                    throw new GradleException( "Unable to find the application image in ${imgOutDir}")
                }
                appImageDir = new File(currImagePath)
            }
        }

        if (jpackageData.skipInstaller) {
            LOGGER.info("Skipping create-installer")
            return
        }
        def installerOutDir = jpackageData.installerOutputDir.get()
        if (imgOutDir != installerOutDir) {
            fileOperations.delete(installerOutDir)
        }
        def taskExecOperations = this.execOperations
        def taskFileOperations = this.fileOperations
        getPackageTypes(jpackageData).each { packageType ->
            if (imgOutDir != installerOutDir) {
                def subdirs = installerOutDir.listFiles({ f -> f.directory } as FileFilter)
                if(subdirs) taskFileOperations.delete(subdirs)
            }
            def standardOutputStream = new ByteArrayOutputStream()
            def result = taskExecOperations.exec {
                ignoreExitValue = true
                standardOutput = standardOutputStream
                def jpackageExec = "${javaHome}/bin/jpackage$EXEC_EXTENSION"
                Util.checkExecutable( jpackageExec)

                def appVersion = jpackageData.appVersion.get()
                def versionOpts = (appVersion == 'unspecified') ? [] : ['--app-version', appVersion]
                if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                    throw new GradleException("The first character of the --app-version argument should be a digit.")
                }

                final def resourceDir = jpackageData.getResourceDir()
                final def resourceOpts = (resourceDir == null) ? [] : [ '--resource-dir', resourceDir ]

                commandLine = [jpackageExec,
                               '--type', packageType,
                               '--dest', jpackageData.installerOutputDir.get(),
                               '--name', jpackageData.installerName.get(),
                               *versionOpts,
                               '--app-image', appImageDir,
                               *resourceOpts,
                               *jpackageData.installerOptions]
            }
            if (result.exitValue != 0) {
                LOGGER.error(standardOutputStream.toString())
            } else {
                LOGGER.info(standardOutputStream.toString())
            }
            standardOutputStream.close()
            result.assertNormalExitValue()
            result.rethrowFailure()
        }
    }

    static List<String> getPackageTypes(JPackageData jpd) {
        if (jpd.installerType) return [jpd.installerType]
        if (OperatingSystem.current().windows) {
            return ['exe', 'msi']
        } else if(OperatingSystem.current().macOsX) {
            return ['pkg', 'dmg']
        } else {
            return ['rpm', 'deb']
        }
    }
}
