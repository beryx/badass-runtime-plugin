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

import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskProvider

import groovy.transform.CompileStatic
import org.beryx.runtime.data.RuntimePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

import org.beryx.runtime.util.Util

import static org.beryx.runtime.util.Util.getEXEC_EXTENSION

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
        if(GradleVersion.current() < GradleVersion.version('7.0')) {
            throw new GradleException("This plugin requires Gradle 7.0 or newer. Try org.beryx.runtime 1.12.7 if you must use an older version of Gradle.")
        }
        project.getPluginManager().apply('application');
        if(hasModuleInfo(project)) {
            throw new GradleException("This plugin works only with non-modular applications.\n" +
                    "For modular applications use https://github.com/beryx/badass-jlink-plugin/.")
        }
        RuntimePluginExtension extension = project.extensions.create(EXTENSION_NAME, RuntimePluginExtension, project)
        String defaultJavaHome = getDefaultJavaHome( project )
        project.tasks.register(TASK_NAME_JRE, JreTask) {
            it.group = 'build'
            it.description = 'Creates a custom java runtime image with jlink'
            it.options.set(extension.options)
            it.additive.set(extension.additive)
            it.modules.set(extension.modules)
            it.javaHome.set(extension.javaHome)
            it.defaultJavaHome.set(defaultJavaHome)
            it.targetPlatforms.set(extension.targetPlatforms)
            it.jreDir.set(extension.jreDir)
            it.dependsOn('jar')
            it.dependsOn(project.configurations['runtimeClasspath'].allDependencies)
        }
        TaskProvider<RuntimeTask> runtimeTask = project.tasks.register( TASK_NAME_RUNTIME, RuntimeTask) {
            it.group = 'build'
            it.description = 'Creates a runtime image of your application'
            it.targetPlatforms.set(extension.targetPlatforms)
            it.launcherData.set(extension.launcherData)
            it.cdsData.set(extension.cdsData)
            it.distDir.set(extension.distDir)
            it.jreDir.set(extension.jreDir)
            it.imageDir.set(extension.imageDir)
            it.dependsOn( TASK_NAME_JRE)
            project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                it.configureStartScripts(taskGraph.hasTask(it))
            }
        }
        project.tasks.register(TASK_NAME_RUNTIME_ZIP, RuntimeZipTask) {
            it.group = 'build'
            it.description = 'Creates a zip of the runtime image of your application'
            it.targetPlatforms.set(extension.targetPlatforms)
            it.imageDir.set(extension.imageDir)
            it.imageZip.set(extension.imageZip)
            it.dependsOn(TASK_NAME_RUNTIME)
        }
        project.tasks.register(TASK_NAME_SUGGEST_MODULES, SuggestModulesTask) {
            it.group = 'build'
            it.description = 'Suggests the modules to be included in the runtime image'
            it.outputs.upToDateWhen { false }
            it.javaHome.set(extension.javaHome)
            it.defaultJavaHome.set(defaultJavaHome)
            it.dependsOn('jar')
        }
        TaskProvider<JPackageImageTask> jpackageImageTask = project.tasks.register(TASK_NAME_JPACKAGE_IMAGE, JPackageImageTask) {
            it.group = 'build'
            it.description = 'Creates an application image using the jpackage tool'
            it.distDir.set(extension.distDir)
            it.jreDir.set(extension.jreDir)
            it.jpackageData.set(extension.jpackageData)
            it.javaHome.set(extension.javaHome)
            it.defaultJavaHome.set(defaultJavaHome)
            it.dependsOn(TASK_NAME_JRE)
        }
        project.tasks.register(TASK_NAME_JPACKAGE, JPackageTask) {
            it.group = 'build'
            it.description = 'Creates an application installer using the jpackage tool'
            it.jpackageData.set(extension.jpackageData)
            it.javaHome.set(extension.javaHome)
            it.defaultJavaHome.set(defaultJavaHome)
            it.dependsOn(TASK_NAME_JPACKAGE_IMAGE)
        }
        project.afterEvaluate {
            TaskProvider<Task> distTask = null
            try{
                distTask = project.tasks.named( 'installShadowDist' )
            }catch (UnknownTaskException ignored) {
                distTask = project.tasks.named( 'installDist' )
            }
            runtimeTask.configure {
                it.dependsOn( distTask )
            }
            jpackageImageTask.configure {
                it.dependsOn( distTask )
            }
        }
    }

    static boolean hasModuleInfo(Project project) {
        Set<File> srcDirs = project.sourceSets.main?.java?.srcDirs
        srcDirs?.any { it.list()?.contains('module-info.java')}
    }

    private static String getDefaultJavaHome(Project project) {
        def value = System.properties['badass.runtime.java.home']
        if(value) return value
        value = System.getenv('BADASS_RUNTIME_JAVA_HOME')
        if(value) return value
        value = Util.getDefaultToolchainJavaHome( project)
        if(value) return value
        value = System.properties['java.home']
        if(['javac', 'jar', 'jlink'].every { new File("$value/bin/$it$EXEC_EXTENSION").file }) return value
        return System.getenv('JAVA_HOME')
    }
}
