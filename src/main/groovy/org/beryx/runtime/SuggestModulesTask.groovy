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
import org.beryx.runtime.data.SuggestModulesData
import org.beryx.runtime.impl.SuggestModulesTaskImpl
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SuggestModulesTask extends BaseTask {
    @Input
    String getJavaHome() {
        javaHomeOrDefault
    }

    SuggestModulesTask() {
        dependsOn('jar')
        description = 'Suggests the modules to be included in the runtime image'
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void suggestMergedModuleInfoAction() {
        def taskData = new SuggestModulesData()
        taskData.javaHome = javaHome
        def taskImpl = objectFactory.newInstance(SuggestModulesTaskImpl, project, taskData)
        taskImpl.execute()
    }
}
