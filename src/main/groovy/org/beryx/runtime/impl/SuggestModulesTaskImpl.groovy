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
package org.beryx.runtime.impl

import org.beryx.runtime.data.SuggestModulesData
import org.beryx.runtime.util.SuggestedModulesBuilder
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.process.ExecOperations

import javax.inject.Inject

abstract class SuggestModulesTaskImpl extends BaseTaskImpl<SuggestModulesData> {
    private static final Logger LOGGER = Logging.getLogger(SuggestModulesTaskImpl)

    @Inject
    SuggestModulesTaskImpl(Project project, SuggestModulesData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        def modules = new SuggestedModulesBuilder(td.javaHome).getProjectModules(project)
        println "modules = [\n${modules.collect {'\'' + it + '\''}.join(',\n')}]"
    }
}
