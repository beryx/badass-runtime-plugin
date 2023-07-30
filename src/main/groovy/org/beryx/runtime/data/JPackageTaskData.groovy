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
package org.beryx.runtime.data

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.beryx.runtime.JreTask
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.os.OperatingSystem

@CompileStatic
@ToString(includeNames = true)
class JPackageTaskData {
    private static final Logger LOGGER = Logging.getLogger(JPackageTaskData.class)

    File distDir

    File jreDir

    File appImageDir

    JPackageData jpackageData

    void configureAppImageDir() {
        final def imgOutDir = jpackageData.imageOutputDirOrDefault
        final def imageName = jpackageData.getImageNameOrDefault()
        final def appImagePath = "${imgOutDir}${File.separator}${imageName}"
        appImageDir = new File(appImagePath)

        if (OperatingSystem.current().macOsX) {
            if (!appImageDir.directory) {
                def currImagePath = "${appImagePath}.app"
                if (!new File(currImagePath).directory) {
                    throw new GradleException("Unable to find the application image in ${imgOutDir}")
                }
                appImageDir = new File(currImagePath)
            }
        }
    }

    void configureRuntimeImageDir(JreTask jreTask) {
        def jlinkPlatforms = jreTask.targetPlatforms.get()
        if (jpackageData.targetPlatformName) {
            if (!jlinkPlatforms.isEmpty()) {
                if (!jlinkPlatforms.keySet().contains(jpackageData.targetPlatformName)) {
                    throw new GradleException("The targetPlatform of the jpackage task ($jpackageData.targetPlatformName) doesn't match any of the targetPlatforms of the jlink task.")
                }
            } else {
                LOGGER.warn("No target platforms defined for the jlink task. The jpackage targetPlatform will be ignored.")
                jpackageData.targetPlatformName = null
            }
        } else {
            if (!jlinkPlatforms.isEmpty()) {
                if (jlinkPlatforms.size() > 1) {
                    throw new GradleException("Since your runtime task is configured to generate images for multiple platforms, you must specify a targetPlatform for your jpackage task.")
                }
                jpackageData.targetPlatformName = jlinkPlatforms.keySet().first()
                LOGGER.warn("No target platform defined for the jpackage task. Defaulting to `$jpackageData.targetPlatformName`.")
            }
        }
        if (jpackageData.targetPlatformName) {
            jreDir = new File(jreTask.jreDirAsFile, "$jreTask.project.name-$jpackageData.targetPlatformName")
        } else {
            jreDir = jreTask.jreDirAsFile
        }
    }

}
