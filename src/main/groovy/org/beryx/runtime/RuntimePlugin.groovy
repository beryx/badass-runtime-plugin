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

import java.util.stream.Collectors

import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider

import groovy.transform.CompileStatic
import org.beryx.runtime.data.RuntimePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

import org.beryx.runtime.util.Util

class RuntimePlugin implements Plugin<Project> {
    final static String EXTENSION_NAME = 'runtime'
    final static String TASK_NAME_JRE = 'jre'
    final static String TASK_NAME_RUNTIME = 'runtime'
    final static String TASK_NAME_RUNTIME_ZIP = 'runtimeZip'
    final static String TASK_NAME_SUGGEST_MODULES = 'suggestModules'
    final static String TASK_NAME_JPACKAGE_IMAGE = 'jpackageImage'
    final static String TASK_NAME_JPACKAGE = 'jpackage'

    @CompileStatic
    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('7.4')) {
            throw new GradleException("This plugin requires Gradle 7.4 or newer. Try org.beryx.runtime 1.13.1 if you must use an older version of Gradle.")
        }
        project.getPluginManager().apply('application');
        if(hasModuleInfo(project)) {
            throw new GradleException("This plugin works only with non-modular applications.\n" +
                    "For modular applications use https://github.com/beryx/badass-jlink-plugin/.")
        }
        RuntimePluginExtension extension = project.extensions.create(EXTENSION_NAME, RuntimePluginExtension, project)
        def jreTask = project.tasks.register(TASK_NAME_JRE, JreTask) {
            it.group = 'build'
            it.description = 'Creates a custom java runtime image with jlink'
            it.projectName = project.getName()
            it.options.set(extension.options)
            it.additive.set(extension.additive)
            it.modules.set(extension.modules)
            it.javaHome.set(extension.javaHome)
            it.targetPlatforms.set(extension.targetPlatforms)
            it.jreDir.set(extension.jreDir)
            Configuration runtimeClasspath = project.configurations.getByName( 'runtimeClasspath')
            def resolvedArtifacts = runtimeClasspath.incoming.getArtifacts().getResolvedArtifacts()
            it.classPathFiles.set(
                    resolvedArtifacts.map(
                            tr -> tr.stream()
                                    .map( ResolvedArtifactResult::getFile )
                                    .collect( Collectors.toSet() )
                    )
            )
            it.dependsOn('jar')
        }
        def runtimeTask = project.tasks.register( TASK_NAME_RUNTIME, RuntimeTask) {
            it.group = 'build'
            it.description = 'Creates a runtime image of your application'
            it.projectName = project.getName()
            it.targetPlatforms.set(extension.targetPlatforms)
            it.launcherData.set(extension.launcherData)
            it.cdsData.set(extension.cdsData)
            it.jreDir.set(extension.jreDir)
            it.imageDir.set(extension.imageDir)
            it.dependsOn( TASK_NAME_JRE)
        }
        project.tasks.register(TASK_NAME_RUNTIME_ZIP, RuntimeZipTask) {
            it.group = 'build'
            it.description = 'Creates a zip of the runtime image of your application'
            it.projectName = project.getName()
            it.targetPlatforms.set(extension.targetPlatforms)
            it.imageDir.set(extension.imageDir)
            it.imageZip.set(extension.imageZip)
            it.dependsOn(TASK_NAME_RUNTIME)
        }
        def suggestTask = project.tasks.register( TASK_NAME_SUGGEST_MODULES, SuggestModulesTask ) {
            it.group = 'build'
            it.description = 'Suggests the modules to be included in the runtime image'
            it.outputs.upToDateWhen { false }
            it.javaHome.set( extension.javaHome )
            Configuration runtimeClasspath = project.configurations.getByName( 'runtimeClasspath' )
            def resolvedArtifacts = runtimeClasspath.incoming.getArtifacts().getResolvedArtifacts()
            it.classPathFiles.set(
                    resolvedArtifacts.map(
                            tr -> tr.stream()
                                    .map( ResolvedArtifactResult::getFile )
                                    .collect( Collectors.toSet() )
                    )
            )
            it.dependsOn( 'jar' )
        }
        def jpackageImageTask = project.tasks.register(TASK_NAME_JPACKAGE_IMAGE, JPackageImageTask) {
            it.group = 'build'
            it.description = 'Creates an application image using the jpackage tool'
            it.jreDir.set(extension.jreDir)
            it.jpackageData.set(extension.jpackageData)
            it.javaHome.set(extension.javaHome)
            it.targetPlatforms.set(extension.targetPlatforms)
            it.projectName = project.getName()
            it.dependsOn(TASK_NAME_JRE)
        }
        project.tasks.register(TASK_NAME_JPACKAGE, JPackageTask) {
            it.group = 'build'
            it.description = 'Creates an application installer using the jpackage tool'
            it.jpackageData.set(extension.jpackageData)
            it.javaHome.set(extension.javaHome)
            it.dependsOn(TASK_NAME_JPACKAGE_IMAGE)
        }
        project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
            runtimeTask.get().configureStartScripts(taskGraph.hasTask(runtimeTask.get()))
        }
        project.afterEvaluate {
            TaskProvider<Sync> distTask = null
            try{
                distTask = project.tasks.named( 'installShadowDist', Sync )
            }catch (UnknownTaskException ignored) {
                distTask = project.tasks.named( 'installDist', Sync )
            }
            def mainDistJarFile = Util.getMainDistJarFile( project )

            def distDirConvention =
                    distTask.flatMap { sync -> project.layout.buildDirectory.dir(sync.destinationDir.path) }
            extension.distDir.convention(distDirConvention)

            jreTask.configure {
                it.mainDistJarFile.set( mainDistJarFile )
                it.dependsOn( distTask )
            }
            suggestTask.configure {
                it.mainDistJarFile.set( mainDistJarFile )
            }
            runtimeTask.configure {
                it.distDir.set(extension.distDir)
                it.dependsOn( distTask )
            }
            jpackageImageTask.configure {
                it.distDir.set(extension.distDir)
                it.mainDistJarFile.set(mainDistJarFile)
                it.dependsOn( distTask )
            }
        }
    }

    static boolean hasModuleInfo(Project project) {
        Set<File> srcDirs = project.sourceSets.main?.java?.srcDirs
        srcDirs?.any { it.list()?.contains('module-info.java')}
    }
}
