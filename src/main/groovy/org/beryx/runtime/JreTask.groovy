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
package org.beryx.runtime

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.process.ExecOperations

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.TargetPlatform
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import org.beryx.runtime.util.SuggestedModulesBuilder

@CompileStatic
abstract class JreTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger(JreTask)

    private final FileOperations fileOperations
    private final ExecOperations execOperations

    @Input
    String projectName

    @Input
    abstract ListProperty<String> getOptions()

    @Input
    abstract Property<Boolean> getAdditive()

    @Input
    abstract ListProperty<String> getModules()

    @Input
    @Optional
    abstract Property<String> getJavaHome()

    @Nested
    abstract MapProperty<String, TargetPlatform> getTargetPlatforms()

    @OutputDirectory
    abstract DirectoryProperty getJreDir()

    @InputFiles
    abstract ListProperty<File> getClassPathFiles();

    @InputFiles
    abstract RegularFileProperty getMainDistJarFile()

    @Inject
    JreTask(FileOperations fileOperations, ExecOperations execOperations) {
        this.fileOperations = fileOperations
        this.execOperations = execOperations
    }

    @TaskAction
    void runtimeTaskAction() {
        def jreDir = jreDir.asFile.get()
        def options = options.get()
        def targetPlatforms = targetPlatforms.get()
        if(targetPlatforms) {
            targetPlatforms.values().each { platform ->
                File jreDirectory = new File(jreDir, "$projectName-$platform.name")
                createJre(jreDirectory, platform.jdkHome.getOrNull(), options + platform.options.get())
            }
        } else {
            createJre(jreDir, javaHome.get(), options)
        }
    }

    @CompileDynamic
    void createJre(File jreDir, String jdkHome, List<String> options) {
        fileOperations.delete(jreDir)

        if(!fileOperations.file("$jdkHome/jmods").directory) {
            throw new GradleException( "Directory not found: $jdkHome/jmods")
        }
        def cmd = ["${javaHome.get()}/bin/jlink",
                   '-v',
                   *options,
                   '--module-path',
                   "$jdkHome/jmods/",
                   '--add-modules', runtimeModules.join(','),
                   '--output', jreDir]
        LOGGER.info("Executing: $cmd")
        def standardOutputStream = new ByteArrayOutputStream()
        def result = execOperations.exec {
            ignoreExitValue = true
            standardOutput = standardOutputStream
            commandLine = cmd
        }
        if(result.exitValue != 0) {
            LOGGER.error(standardOutputStream.toString())
        } else {
            LOGGER.info(standardOutputStream.toString())
        }
        standardOutputStream.close()
        result.assertNormalExitValue()
        result.rethrowFailure()
    }

    @CompileStatic @Internal
    Collection<String> getRuntimeModules() {
        Set<String> imageModules = []
        def modules = modules.get()
        if ( additive.get() || !modules ) {
            imageModules.addAll(
                    new SuggestedModulesBuilder(
                            javaHome.get()
                    ).getProjectModules(
                            mainDistJarFile.get(),
                            classPathFiles.get()
                    )
            )
        }
        if ( modules ) {
            imageModules.addAll( modules )
        }
        imageModules
    }
}
