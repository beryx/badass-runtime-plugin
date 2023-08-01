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

import org.beryx.runtime.util.Util
import org.gradle.api.tasks.Internal

import static org.beryx.runtime.util.Util.EXEC_EXTENSION

import groovy.transform.CompileStatic
import groovy.transform.ToString

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

@CompileStatic
@ToString(includeNames = true)
class JPackageData {
    private final Project project
    private final LauncherData launcherData

    @Input
    String jpackageHome

    @Input
    String outputDir = 'jpackage'

    @Internal
    File imageOutputDir

    @Internal
    String imageName

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

    @Internal
    File installerOutputDir

    @Internal
    String installerName

    @Input @Optional
    String appVersion

    @Input
    List<String> installerOptions = []

    @Internal
    List<String> args = []

    @Internal
    List<String> jvmArgs = []

    @Input @Optional
    String mainJar

    @Internal
    String mainClass

    JPackageData(Project project, LauncherData launcherData) {
        this.project = project
        this.launcherData = launcherData
        this.jpackageHome = ''
    }

    @Input
    List<String> getArgsOrDefault() {
        this.@args ?: Util.getDefaultArgs(project)
    }

    @Input
    List<String> getJvmArgsOrDefault() {
        this.@jvmArgs ?: launcherData.jvmArgsOrDefault
    }

    @Input
    String getMainClassOrDefault() {
        this.@mainClass ?: Util.getMainClass(project)
    }
    @Input
    String getImageNameOrDefault() {
        this.@imageName ?: project.name
    }

    @Input
    String getInstallerNameOrDefault() {
        this.@installerName ?: project.name
    }

    @OutputDirectory
    File getImageOutputDirOrDefault() {
        this.@imageOutputDir ?: project.file("$project.buildDir/$outputDir")
    }

    @OutputDirectory
    File getInstallerOutputDirOrDefault() {
        this.@installerOutputDir ?: project.file("$project.buildDir/$outputDir")
    }

    @Internal
    String getDefaultJPackageHome() {
        def value = System.properties['badass.runtime.jpackage.home']
        if(value) return value
        value = System.getenv('BADASS_RUNTIME_JPACKAGE_HOME')
        if(value) return value
        value = Util.getDefaultToolchainJavaHome(project)
        if(value) return value
        value = System.properties['java.home']
        if(new File("$value/bin/jpackage$EXEC_EXTENSION").file) return value
        return System.getenv('JAVA_HOME')
    }
}
