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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.runtime.data.TargetPlatform
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
abstract class RuntimeZipTask extends DefaultTask {

    @Input
    String projectName

    @Nested
    abstract MapProperty<String, TargetPlatform> getTargetPlatforms()

    @InputDirectory
    abstract DirectoryProperty getImageDir()

    @OutputFile
    abstract RegularFileProperty getImageZip()

    @TaskAction
    void runtimeZipTaskAction() {
        def targetPlatforms = targetPlatforms.get()
        def imageDir = imageDir.asFile.get()
        def imageZip = imageZip.asFile.get()
        if(targetPlatforms) {
            def zipDir = imageZip.parentFile
            def zipName = imageZip.name
            int pos = zipName.lastIndexOf('.')
            def ext = (pos > 0) ? zipName.substring(pos+1) : 'zip'
            def baseName = (pos > 0) ? zipName.substring(0,pos) : zipName
            targetPlatforms.values().each { platform ->
                File zipFile = new File(zipDir, "${baseName}-${platform.name}.${ext}")
                File imageDirectory = new File(imageDir, "$projectName-$platform.name")
                createZip(imageDirectory, zipFile)
            }
        } else {
            createZip(imageDir, imageZip)
        }
    }

    @CompileDynamic
    private void createZip(File imageDir, File zipFile) {
        def parentPath = imageDir.parentFile.toPath()
        ant.zip(destfile: zipFile, duplicate: 'fail') {
            imageDir.eachFileRecurse { f ->
                int mode = f.canExecute() ? 755 : 644
                def relPath = parentPath.relativize(f.toPath()).toString()
                zipfileset(dir: parentPath, includes: relPath, filemode: mode)
            }
        }
    }
}
