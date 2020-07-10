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

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.beryx.runtime.util.Util
import org.gradle.api.Project
import org.gradle.api.tasks.Input

@CompileStatic
@ToString(includeNames = true)
class LauncherData implements Serializable {
    transient private final Project project

    private List<String> jvmArgs = []

    LauncherData(Project project) {
        this.project = project
    }

    @Input
    File unixScriptTemplate

    @Input
    File windowsScriptTemplate

    @Input
    List<String> getJvmArgs() {
        this.@jvmArgs ?: Util.getDefaultJvmArgs(project)
    }
}
