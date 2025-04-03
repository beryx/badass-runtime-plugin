/*
 * Copyright 2020 the original author or authors.
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

import org.gradle.api.provider.ListProperty

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.beryx.runtime.RuntimePlugin
import org.beryx.runtime.util.Util
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

@CompileStatic
@ToString(includeNames = true)
abstract class LauncherData {

    @Input
    abstract ListProperty<String> getJvmArgs()

    @Inject
    LauncherData(Project project) {
        jvmArgs.convention(Util.getDefaultJvmArgs(project))
    }

    @Input
    boolean noConsole = false

    @Input
    boolean runInBinDir = false

    @InputFile @Optional
    File unixScriptTemplate

    @InputFile @Optional
    File windowsScriptTemplate

    @Internal
    URL getUnixTemplateUrl() {
        if(unixScriptTemplate) return unixScriptTemplate.toURI().toURL()
        def resourceName = '/unixScriptTemplate.txt'
        def templateUrl = RuntimePlugin.class.getResource(resourceName)
        if(!templateUrl) throw new GradleException("Resource $resourceName not found.")
        return templateUrl
    }

    @Internal
    URL getWindowsTemplateUrl() {
        if(windowsScriptTemplate) return windowsScriptTemplate.toURI().toURL()
        def resourceName = noConsole ? '/windowsScriptTemplateJavaw.txt' : '/windowsScriptTemplate.txt'
        def templateUrl = RuntimePlugin.class.getResource(resourceName)
        if(!templateUrl) throw new GradleException("Resource $resourceName not found.")
        return templateUrl
    }
}
