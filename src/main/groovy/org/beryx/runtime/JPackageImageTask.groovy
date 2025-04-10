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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.JPackageData
import org.beryx.runtime.data.TargetPlatform
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

import org.beryx.runtime.util.Util

import static org.beryx.runtime.util.Util.EXEC_EXTENSION

@CompileStatic
abstract class JPackageImageTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger( JPackageImageTask.class)

    private final FileOperations fileOperations
    private final ExecOperations execOperations

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

    @InputFile
    abstract Property<File> getMainDistJarFile()

    @Nested
    abstract MapProperty<String, TargetPlatform> getTargetPlatforms()

    @Input
    String projectName

    @Inject
    JPackageImageTask(FileOperations fileOperations, ExecOperations execOperations){
        this.fileOperations = fileOperations
        this.execOperations = execOperations
    }

    @TaskAction
    @CompileDynamic
    void jpackageTaskAction() {
        def distDir = distDir.get().asFile
        def jpackageData = jpackageData.get()
        def jlinkPlatforms = targetPlatforms.get()
        def jreDirectory = jreDir.get()
        def mainDistJarFile = mainDistJarFile.get()

        if (jpackageData.targetPlatformName) {
            if (!jlinkPlatforms.isEmpty()) {
                if (!jlinkPlatforms.keySet().contains(jpackageData.targetPlatformName)) {
                    throw new GradleException( "The targetPlatform of the jpackage task ($jpackageData.targetPlatformName) doesn't match any of the targetPlatforms of the jlink task.")
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
        File jreDir
        if (jpackageData.targetPlatformName) {
            jreDir = new File(jreDirectory.asFile, "$projectName-$jpackageData.targetPlatformName")
        } else {
            jreDir = jreDirectory.asFile
        }

        def standardOutputStream = new ByteArrayOutputStream()
        def taskFileOperations = this.fileOperations
        def result = execOperations.exec {
            ignoreExitValue = true
            standardOutput = standardOutputStream
            def outputDir = jpackageData.imageOutputDir.get()
            taskFileOperations.delete(outputDir)

            def jpackageExec = "${javaHome.get()}/bin/jpackage$EXEC_EXTENSION"
            Util.checkExecutable( jpackageExec)

            LOGGER.info("input subdir: $distDir.name")

            def appVersion = jpackageData.appVersion.get()
            def versionOpts = (appVersion == 'unspecified') ? [] : ['--app-version', appVersion]
            if (versionOpts && (!appVersion || !Character.isDigit(appVersion[0] as char))) {
                throw new GradleException("The first character of the --app-version argument should be a digit.")
            }

            final def resourceDir = jpackageData.getResourceDir()
            final def resourceOpts = resourceDir == null ? [] : [ '--resource-dir', resourceDir ]

            final def jvmArgs = (jpackageData.jvmArgs.isPresent() ? jpackageData.jvmArgs.get().collect{['--java-options', adjustArg(it) ]}.flatten() : [])
            final def args = (jpackageData.args.isPresent() ? jpackageData.args.get().collect{['--arguments', adjustArg(it)]}.flatten() : [])

            commandLine = [jpackageExec,
                           '--type', 'app-image',
                           '--input', "$distDir${File.separator}lib",
                           '--main-jar', jpackageData.mainJar ?: mainDistJarFile.name,
                           '--main-class', jpackageData.mainClass.get(),
                           '--dest', outputDir,
                           '--name', jpackageData.imageName.get(),
                           *versionOpts,
                           '--runtime-image', jreDir,
                           *resourceOpts,
                           *jvmArgs,
                           *args,
                           *jpackageData.imageOptions]
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

    static String adjustArg(String arg) {
        def adjusted = arg.replace('"', '\\"')
        if (!(adjusted ==~ /[\w\-\+=\/\\,;.:#]+/)) {
            adjusted = '"' + adjusted + '"'
        }
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8227641
        adjusted = adjusted.replace(' ', '\\" \\"')
        adjusted = adjusted.replace('{{BIN_DIR}}', '$APPDIR' + File.separator + '..')
        adjusted
    }
}
