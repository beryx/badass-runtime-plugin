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

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

import groovy.transform.CompileStatic
import org.beryx.runtime.util.Util

@CompileStatic
abstract class RuntimePluginExtension {
    private final ObjectFactory objects
    private final FileOperations fileOperations
    private final DirectoryProperty buildDirectory

    abstract DirectoryProperty getDistDir()
    abstract DirectoryProperty getJreDir()
    abstract DirectoryProperty getImageDir()
    abstract RegularFileProperty getImageZip()

    abstract ListProperty<String> getOptions()
    abstract Property<Boolean> getAdditive()
    abstract ListProperty<String> getModules()

    abstract Property<String> getJavaHome()
    abstract MapProperty<String, TargetPlatform> getTargetPlatforms()
    abstract Property<Integer> getJvmVersion()

    @Nested
    abstract Property<LauncherData> getLauncherData()

    @Nested
    abstract Property<JPackageData> getJpackageData()

    @Nested
    abstract Property<CdsData> getCdsData()

    @Inject
    RuntimePluginExtension(Project project, FileOperations fileOperations) {
        this.objects = project.objects
        this.fileOperations = fileOperations
        this.buildDirectory = project.layout.buildDirectory
        jreDir.convention(buildDirectory.dir('jre'))
        imageDir.convention(buildDirectory.dir('image'))
        imageZip.convention(buildDirectory.file('image.zip'))
        options.convention(new ArrayList<String>())
        additive.convention(false)
        modules.convention(new ArrayList<String>())
        javaHome.convention( Util.getDefaultJavaHome(project))
        launcherData.convention(objects.newInstance( LauncherData, project ))
        jpackageData.convention(objects.newInstance( JPackageData, project, launcherData.get() ))
        cdsData.convention(objects.newInstance( CdsData ))
    }

    void addOptions(String... option) {
        options.addAll(option.toList())
    }

    void addModules(String... module) {
        modules.addAll(module.toList())
    }

    void targetPlatform(String name, String jdkHome, List<String> options = []) {
        targetPlatform(name) { TargetPlatform platform ->
            platform.jdkHome.set(jdkHome)
            platform.options.addAll(options)
        }
    }

    void targetPlatform(String name, Action<TargetPlatform> action) {
        def targetPlatform = objects.newInstance( TargetPlatform, fileOperations, buildDirectory, name)
        targetPlatforms.put(name, targetPlatform)
        action.execute(targetPlatform)
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
}
