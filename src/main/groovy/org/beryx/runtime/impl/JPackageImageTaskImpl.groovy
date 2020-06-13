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

@CompileStatic
class JPackageImageTaskImpl extends BaseTaskImpl<JPackageTaskData> {
    private static final Logger LOGGER = Logging.getLogger(JPackageImageTaskImpl.class)

    JPackageImageTaskImpl(Project project, JPackageTaskData taskData) {
        super(project, taskData)
        LOGGER.debug("taskData: $taskData")
    }

    @CompileDynamic
    void execute() {
        def result = project.exec {
            ignoreExitValue = true
            standardOutput = new ByteArrayOutputStream()

            project.ext.jpackageImageOutput = {
                return standardOutput.toString()
            }

            def jpd = td.jpackageData
            def outputDir = jpd.imageOutputDir
            project.delete(outputDir)

            def jpackageExec = "$jpd.jpackageHome/bin/jpackage$EXEC_EXTENSION"
            Util.checkExecutable(jpackageExec)

            def inputSuffix = project.tasks.findByName('installShadowDist') ? '-shadow' : ''
            LOGGER.info("input subdir: $project.name$inputSuffix")

            def appVersion = (jpd.appVersion ?: project.version).toString()
            def versionOpts = (appVersion == 'unspecified') ? [] : ['--app-version', appVersion]
            if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                throw new GradleException("The first character of the --app-version argument should be a digit.")
            }

            final def resourceDir = jpd.getResourceDir()
            final def resourceOpts = resourceDir == null ? [] : [ '--resource-dir', resourceDir ]

            final def jvmArgs = (jpd.jvmArgs ? jpd.jvmArgs.collect{[ '--java-options', adjustArg(it) ]}.flatten() : [])
            final def args = (jpd.args ? jpd.args.collect{['--arguments', adjustArg(it)]}.flatten() : [])

            commandLine = [jpackageExec,
                           '--type', 'app-image',
                           '--input', "$td.distDir${File.separator}lib",
                           '--main-jar', jpd.mainJar ?: Util.getMainDistJarFile(project).name,
                           '--main-class', jpd.mainClass,
                           '--dest', outputDir,
                           '--name', jpd.imageName,
                           *versionOpts,
                           '--runtime-image', td.jreDir,
                           *resourceOpts,
                           *jvmArgs,
                           *args,
                           *jpd.imageOptions]
        }

        if (result.exitValue != 0) {
            LOGGER.error(project.ext.jpackageImageOutput())
        } else {
            LOGGER.info(project.ext.jpackageImageOutput())
        }

        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    static String adjustArg(String arg) {
        def adjusted = arg.replace('"', '\\"')
        if (!(adjusted ==~ /[\w\-\+=\/\\,;.:#]+/)) {
            adjusted = '"' + adjusted + '"'
        }
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8227641
        adjusted = adjusted.replace(' ', '\\" \\"')
        adjusted
    }
}
