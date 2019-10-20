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
package org.beryx.runtime.util

import groovy.io.FileType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.codehaus.groovy.tools.Utilities
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GradleVersion

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class Util {
    static String EXEC_EXTENSION = System.getProperty('os.name', '').toLowerCase().contains('win') ? '.exe' : ''

    static boolean isValidClassFileReference(String name) {
        if(!name.endsWith('.class')) return false
        name = name - '.class'
        name = name.split('[./\\\\]')[-1]
        return Utilities.isJavaIdentifier(name)
    }

    static void scan(File file,
         @ClosureParams(value= SimpleType, options="java.lang.String,java.lang.String,java.io.InputStream") Closure<Void> action) {
        if(!file.exists()) throw new IllegalArgumentException("File or directory not found: $file")
        if(file.directory) scanDir(file, action)
        else scanJar(file, action)
    }

    private static void scanDir(File dir,
                        @ClosureParams(value= SimpleType, options="java.lang.String,java.lang.String,java.io.InputStream") Closure<Void> action) {
        if(!dir.directory) throw new IllegalArgumentException("Not a directory: $dir")
        dir.eachFileRecurse(FileType.FILES) { file ->
            def basePath = dir.absolutePath.replace('\\', '/')
            def relPath = dir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            action.call(basePath, relPath, file.newInputStream())
        }
    }

    private static void scanJar(File jarFile,
                        @ClosureParams(value= SimpleType, options="java.lang.String,java.lang.String,java.io.InputStream") Closure<Void> action) {
        def zipFile = new ZipFile(jarFile)
        zipFile.entries().each { ZipEntry entry -> action.call('', entry.name, zipFile.getInputStream(entry)) }
    }

    static File getArtifact(ResolvedDependency dep) {
        def artifact = dep.moduleArtifacts.find {it.classifier} ?: dep.moduleArtifacts[0]
        artifact.file
    }

    @CompileDynamic
    static DirectoryProperty createDirectoryProperty(Project project) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            return project.layout.directoryProperty()
        } else {
            return project.objects.directoryProperty()
        }
    }

    @CompileDynamic
    static RegularFileProperty createRegularFileProperty(Project project) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            return project.layout.fileProperty()
        } else {
            return project.objects.fileProperty()
        }
    }

    static <T> void addToListProperty(ListProperty<T> listProp, T... values) {
        if(GradleVersion.current() < GradleVersion.version('5.0-milestone-1')) {
            def list = new ArrayList(listProp.get())
            list.addAll(values as List)
            listProp.set(list)
        } else {
            listProp.addAll(values as List)
        }
    }

    @CompileDynamic
    static <K,V>Provider<Map<K,V>> createMapProperty(Project project,
                                             Class<K> keyType, Class<V> valueType) {
        Provider<Map<K,V>> provider
        if(GradleVersion.current() < GradleVersion.version('5.1')) {
            provider = (Property<Map<K,V>>)project.objects.property(Map)
        } else {
            provider = project.objects.mapProperty(keyType, valueType)
        }
        provider.set(new TreeMap<K,V>())
        provider
    }

    @CompileDynamic
    static <K,V> void putToMapProvider(Provider<Map<K,V>> mapProvider, K key, V value) {
        def map = new TreeMap(mapProvider.get())
        map[key] = value
        mapProvider.set(map)
    }

    static void checkExecutable(String filePath) {
        checkExecutable(new File(filePath))
    }

    static void checkExecutable(File f) {
        if(!f.file) throw new GradleException("$f.absolutePath does not exist.")
        if(!f.canExecute()) throw new GradleException("$f.absolutePath is not executable.")
    }

    @CompileDynamic
    public static File getArchiveFile(Project project) {
        Jar jarTask = (Jar)project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)
        if(GradleVersion.current() < GradleVersion.version('5.1')) {
            return jarTask.archivePath
        } else {
            return jarTask.archiveFile.get().asFile
        }
    }

    public static File getMainDistJarFile(Project project) {
        File jarFile = getArchiveFile(project)
        if(project.tasks.findByName('installShadowDist')) {
            def baseName = jarFile.name - '.jar'
            jarFile = new File(jarFile.parent, "$baseName-all.jar")
        }
        jarFile
    }
}
