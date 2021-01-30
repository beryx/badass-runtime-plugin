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
package org.beryx.runtime.impl

import static org.beryx.runtime.util.Util.EXEC_EXTENSION

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.beryx.runtime.data.JPackageTaskData
import org.beryx.runtime.util.Util
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.os.OperatingSystem

@CompileStatic
class JPackageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageTaskImpl.class);

    JPackageTaskImpl(Project project, JPackageTaskData taskData) {
        super(project, taskData)
        LOGGER.debug("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        final def jpd = td.jpackageData
        if (jpd.skipInstaller) {
            LOGGER.info("Skipping create-installer")
            return
        }

        if (jpd.getImageOutputDir() != jpd.getInstallerOutputDir()) {
            project.delete(project.files(jpd.getInstallerOutputDir()))
        }
        packageTypes.each { packageType ->
            if (jpd.getImageOutputDir() != jpd.getInstallerOutputDir()) {
                def subdirs = jpd.getInstallerOutputDir().listFiles({ f -> f.directory } as FileFilter)
                if(subdirs) project.delete(subdirs)
            }
            def result = project.exec {
                ignoreExitValue = true
                standardOutput = new ByteArrayOutputStream()
                project.ext.jpackageInstallerOutput = {
                    return standardOutput.toString()
                }
                def jpackageExec = "${jpd.getJPackageHomeOrDefault()}/bin/jpackage$EXEC_EXTENSION"
                Util.checkExecutable(jpackageExec)

                def appVersion = (jpd.appVersion ?: project.version).toString()
                def versionOpts = (appVersion == 'unspecified') ? [] : ['--app-version', appVersion]
                if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                    throw new GradleException("The first character of the --app-version argument should be a digit.")
                }

                final def resourceDir = jpd.getResourceDir()
                final def resourceOpts = (resourceDir == null) ? [] : [ '--resource-dir', resourceDir ]

                commandLine = [jpackageExec,
                               '--type', packageType,
                               '--dest', jpd.getInstallerOutputDir(),
                               '--name', jpd.installerName,
                               *versionOpts,
                               '--app-image', td.appImageDir,
                               *resourceOpts,
                               *jpd.installerOptions]
            }

            if (result.exitValue != 0) {
                LOGGER.error(project.ext.jpackageInstallerOutput())
            } else {
                LOGGER.info(project.ext.jpackageInstallerOutput())
            }

            result.assertNormalExitValue()
            result.rethrowFailure()
        }
    }

    List<String> getPackageTypes() {
        def jpd = td.jpackageData
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
