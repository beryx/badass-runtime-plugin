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

import javax.inject.Inject

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import org.beryx.runtime.util.Util

import groovy.transform.CompileStatic
import groovy.transform.ToString

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory


@CompileStatic
@ToString(includeNames = true)
abstract class JPackageData {
    @Input
    abstract Property<String> getJpackageHome()

    @Input
    abstract Property<String> getOutputDir()

    @OutputDirectory
    abstract DirectoryProperty getImageOutputDir()

    @Input
    abstract Property<String> getImageName()

    @Input
    List<String> imageOptions = []

    @InputDirectory @Optional
    File resourceDir

    @Input @Optional
    String targetPlatformName

    @Input
    boolean skipInstaller = false

    @Input @Optional
    String installerType

    @OutputDirectory
    abstract DirectoryProperty getInstallerOutputDir()

    @Input
    abstract Property<String> getInstallerName()

    @Input @Optional
    String appVersion

    @Input
    List<String> installerOptions = []

    @Input @Optional
    abstract ListProperty<String> getArgs()

    @Input @Optional
    abstract ListProperty<String> getJvmArgs()

    @Input @Optional
    String mainJar

    @Input
    abstract Property<String> getMainClass()

    @Inject
    JPackageData(Project project, LauncherData launcherData) {
        jpackageHome.convention('')
        outputDir.convention('jpackage')
        imageOutputDir.convention(project.layout.buildDirectory.dir(outputDir))
        imageName.convention( project.name )
        installerOutputDir.convention(project.layout.buildDirectory.dir(outputDir))
        installerName.convention( project.name )
        args.convention(Util.getDefaultArgs(project))
        jvmArgs.convention(launcherData.jvmArgs)
        mainClass.convention(Util.getMainClass(project))
    }
}
