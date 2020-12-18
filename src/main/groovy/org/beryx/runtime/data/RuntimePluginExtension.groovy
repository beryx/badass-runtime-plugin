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
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

import groovy.transform.CompileStatic

@CompileStatic
class RuntimePluginExtension {
    private final Project project

    final DirectoryProperty distDir
    final DirectoryProperty jreDir
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip

    final ListProperty<String> options
    final Property<Boolean> additive
    final ListProperty<String> modules
    final Property<String> javaHome
    final Provider<Map<String, TargetPlatform>> targetPlatforms
    final Property<Integer> jvmVersion

    final Property<LauncherData> launcherData
    final Property<JPackageData> jpackageData
    final Property<CdsData> cdsData

    RuntimePluginExtension(Project project) {
        this.project = project
        distDir = Util.createDirectoryProperty(project)

        jreDir = Util.createDirectoryProperty(project)
        jreDir.set(project.layout.buildDirectory.dir('jre'))

        imageDir = Util.createDirectoryProperty(project)
        imageDir.set(project.layout.buildDirectory.dir('image'))

        imageZip = Util.createRegularFileProperty(project)
        imageZip.set(project.layout.buildDirectory.file('image.zip'))

        options = project.objects.listProperty(String)
        options.set(new ArrayList<String>())

        additive = project.objects.property(Boolean)
        additive.set(false)

        modules = project.objects.listProperty(String)
        modules.set(new ArrayList<String>())

        javaHome = project.objects.property(String)
        javaHome.set(getDefaultJavaHome())

        targetPlatforms = Util.createMapProperty(project, String, TargetPlatform)

        jvmVersion = project.objects.property(Integer)

        launcherData = project.objects.property(LauncherData)
        def ld = new LauncherData(project)
        launcherData.set(ld)

        jpackageData = project.objects.property(JPackageData)
        def jpd = new JPackageData(project, ld)
        jpackageData.set(jpd)

        cdsData = project.objects.property(CdsData)
        cdsData.set(new CdsData())
    }

    void addOptions(String... options) {
        Util.addToListProperty(this.options, options)
    }

    void addModules(String... modules) {
        Util.addToListProperty(this.modules, modules)
    }

    void targetPlatform(String name, String jdkHome, List<String> options = []) {
        Util.putToMapProvider(targetPlatforms, name, new TargetPlatform(project, name, jdkHome, options))
    }

    void targetPlatform(String name, Action<TargetPlatform> action) {
        def targetPlatform = new TargetPlatform(project, name)
        action.execute(targetPlatform)
        Util.putToMapProvider(targetPlatforms, name, targetPlatform)
    }

    void enableCds(Action<CdsData> action = null) {
        cdsData.get().enabled = true
        if(action) {
            action.execute(cdsData.get())
        }
    }

    void launcher(Action<LauncherData> action) {
        action.execute(launcherData.get())
    }

    void jpackage(Action<JPackageData> action) {
        action.execute(jpackageData.get())
    }

    private static String getDefaultJavaHome() {
        def value = System.properties['badass.runtime.java.home']
        if(value) return value
        value = System.getenv('BADASS_RUNTIME_JAVA_HOME')
        if(value) return value
        value = System.properties['java.home']
        String ext = System.getProperty('os.name', '').toLowerCase().contains('win') ? '.exe' : ''
        if(['javac', 'jar', 'jlink'].every { new File("$value/bin/$it$ext").file }) return value
        return System.getenv('JAVA_HOME')
    }
}
