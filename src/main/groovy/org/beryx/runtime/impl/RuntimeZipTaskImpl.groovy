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

import org.beryx.runtime.data.RuntimeZipTaskData
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import javax.inject.Inject

abstract class RuntimeZipTaskImpl extends BaseTaskImpl<RuntimeZipTaskData> {
    private static final Logger LOGGER = Logging.getLogger(RuntimeZipTaskImpl)

    @Inject
    RuntimeZipTaskImpl(Project project, RuntimeZipTaskData taskData) {
        super(project, taskData)
        LOGGER.info("taskData: $taskData")
    }

    void execute() {
        if(td.targetPlatforms) {
            def zipDir = td.imageZip.parentFile
            def zipName = td.imageZip.name
            int pos = zipName.lastIndexOf('.')
            def ext = (pos > 0) ? zipName.substring(pos+1) : 'zip'
            def baseName = (pos > 0) ? zipName.substring(0,pos) : zipName
            td.targetPlatforms.values().each { platform ->
                File zipFile = new File(zipDir, "${baseName}-${platform.name}.${ext}")
                File imageDir = new File(td.imageDir, "$project.name-$platform.name")
                createZip(imageDir, zipFile)
            }
        } else {
            createZip(td.imageDir, td.imageZip)
        }
    }

    protected void createZip(File imageDir, File zipFile) {
        def parentPath = imageDir.parentFile.toPath()
        project.ant.zip(destfile: zipFile, duplicate: 'fail') {
            imageDir.eachFileRecurse { f ->
                int mode = f.canExecute() ? 755 : 644
                def relPath = parentPath.relativize(f.toPath()).toString()
                zipfileset(dir: parentPath, includes: relPath, filemode: mode)
            }
        }
    }
}
