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
import groovy.transform.ToString

@CompileStatic
@ToString(includeNames = true)
class RuntimePluginExtension {
    final DirectoryProperty distDir
    final DirectoryProperty jreDir
    final DirectoryProperty imageDir
    final RegularFileProperty imageZip

    final ListProperty<String> options
    final ListProperty<String> modules
    final Property<String> javaHome
    final Provider<Map<String, TargetPlatform>> targetPlatforms
    final Property<Integer> jvmVersion

    final Property<JPackageData> jpackageData

    RuntimePluginExtension(Project project) {
        distDir = Util.createDirectoryProperty(project)

        jreDir = Util.createDirectoryProperty(project)
        jreDir.set(project.layout.buildDirectory.dir('jre'))

        imageDir = Util.createDirectoryProperty(project)
        imageDir.set(project.layout.buildDirectory.dir('image'))

        imageZip = Util.createRegularFileProperty(project)
        imageZip.set(project.layout.buildDirectory.file('image.zip'))

        options = project.objects.listProperty(String)
        options.set(new ArrayList<String>())

        modules = project.objects.listProperty(String)
        modules.set(new ArrayList<String>())

        javaHome = project.objects.property(String)
        javaHome.set(getDefaultJavaHome())

        targetPlatforms = Util.createMapProperty(project, String, TargetPlatform)

        jvmVersion = project.objects.property(Integer)

        jpackageData = project.objects.property(JPackageData)
        def jpd = new JPackageData(project)
        jpackageData.set(jpd)
    }

    void addOptions(String... options) {
        Util.addToListProperty(this.options, options)
    }

    void addModules(String... modules) {
        Util.addToListProperty(this.modules, modules)
    }

    void targetPlatform(String name, String jdkHome, List<String> options = []) {
        Util.putToMapProvider(targetPlatforms, name, new TargetPlatform(name, jdkHome, options))
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
