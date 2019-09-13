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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.JPackageTaskData
import org.beryx.runtime.util.Util
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static org.beryx.runtime.util.Util.EXEC_EXTENSION

@CompileStatic
class JPackageImageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageImageTaskImpl.class)

    JPackageImageTaskImpl(Project project, JPackageTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        LOGGER.warn("The jpackage task is experimental. Use it at your own risk.")
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()
            project.ext.jpackageImageOutput = {
                return standardOutput.toString()
            }
            def outputDir = td.jpackageData.imageOutputDir
            project.delete(outputDir)
            def jpd = td.jpackageData
            def jpackageExec = "$jpd.jpackageHome/bin/jpackage$EXEC_EXTENSION"
            Util.checkExecutable(jpackageExec)
            def inputSuffix = project.tasks.findByName('installShadowDist') ? '-shadow' : ''
            LOGGER.info("input subdir: $project.name$inputSuffix")
            commandLine = [jpackageExec,
                           '--input', "$td.runtimeImageDir${File.separator}lib",
                           '--main-jar', jpd.mainJar ?: Util.getMainDistJarFile(project).name,
                           '--main-class', jpd.mainClass,
                           '--output', outputDir,
                           '--name', jpd.imageName,
                           '--app-version', jpd.appVersion ?: project.version,
                           '--runtime-image', td.runtimeImageDir,
                           '--resource-dir', jpd.getResourceDir(),
                           *(jpd.jvmArgs ? jpd.jvmArgs.collect{['--java-options', adjustArg(it)]}.flatten() : []),
                           *jpd.imageOptions]
        }
        if(result.exitValue != 0) {
            LOGGER.error(project.ext.jpackageImageOutput())
        } else {
            LOGGER.info(project.ext.jpackageImageOutput())
        }
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    static String adjustArg(String arg) {
        def adjusted = arg.replace('"', '\\"')
        if(!(adjusted ==~ /[\w\-\+=\/\\,;.:#]+/)) {
            adjusted = '"' + adjusted + '"'
        }
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8227641
        adjusted = adjusted.replace(' ', '\\" \\"')
        adjusted
    }
}
