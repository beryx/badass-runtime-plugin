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
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import static org.beryx.runtime.util.Util.EXEC_EXTENSION

@CompileStatic
@ToString(includeNames = true)
class JPackageData {
    private final Project project

    @Input
    String jpackageHome

    @Input
    String outputDir = 'jpackage'

    File imageOutputDir

    String imageName

    @Input
    List<String> imageOptions = []

    @Input @Optional
    File resourceDir

    @Input @Optional
    String targetPlatformName

    @Input
    boolean skipInstaller = false

    @Input @Optional
    String installerType

    File installerOutputDir

    String installerName

    @Input @Optional
    String identifier

    @Input @Optional
    String appVersion

    @Input
    List<String> installerOptions = []

    @Input
    List<String> jvmArgs = []

    @Input @Optional
    String mainJar

    String mainClass

    JPackageData(Project project) {
        this.project = project
        this.jpackageHome = defaultJPackageHome
    }

    @Input
    String getMainClass() {
        this.@mainClass ?: defaultMainClass
    }

    @Input
    String getImageName() {
        this.@imageName ?: project.name
    }

    @Input
    String getInstallerName() {
        this.@installerName ?: project.name
    }

    @InputDirectory
    File getResourceDir() {
        this.@resourceDir
    }

    @OutputDirectory
    File getImageOutputDir() {
        this.@imageOutputDir ?: project.file("$project.buildDir/$outputDir")
    }

    @OutputDirectory
    File getInstallerOutputDir() {
        this.@installerOutputDir ?: project.file("$project.buildDir/$outputDir")
    }


    @Internal
    private String getDefaultMainClass() {
        def mainClass = project['mainClassName'] as String
        int pos = mainClass.lastIndexOf('/')
        if(pos < 0) return mainClass
        mainClass.substring(pos + 1)
    }

    private static String getDefaultJPackageHome() {
        def value = System.properties['badass.runtime.jpackage.home']
        if(value) return value
        value = System.getenv('BADASS_RUNTIME_JPACKAGE_HOME')
        if(value) return value
        value = System.properties['java.home']
        if(new File("$value/bin/jpackage$EXEC_EXTENSION").file) return value
        return System.getenv('JAVA_HOME')
    }
}
