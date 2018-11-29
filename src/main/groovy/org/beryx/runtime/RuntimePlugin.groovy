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

import groovy.transform.CompileStatic
import org.beryx.runtime.data.RuntimePluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

@CompileStatic
class RuntimePlugin implements Plugin<Project> {
    final static String EXTENSION_NAME = 'runtime'
    final static String TASK_NAME_RUNTIME = 'runtime'
    final static String TASK_NAME_RUNTIME_ZIP = 'runtimeZip'
    final static String TASK_NAME_SUGGEST_MODULES = 'suggestModules'

    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            throw new GradleException("This version of the plugin requires Gradle 5 or newer.\n" +
                "Upgrade to Gradle 5 or use a version with the '-gradle4' suffix.")
        }
        project.getPluginManager().apply('application');
        def extension = project.extensions.create(EXTENSION_NAME, RuntimePluginExtension, project)
        project.getTasks().create(TASK_NAME_RUNTIME, RuntimeTask, { it.init(extension) })
        project.getTasks().create(TASK_NAME_RUNTIME_ZIP, RuntimeZipTask, { it.init(extension) })
        project.getTasks().create(TASK_NAME_SUGGEST_MODULES, SuggestModulesTask, { it.init(extension) })
    }
}
