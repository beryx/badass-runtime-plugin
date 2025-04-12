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

import org.gradle.api.UnknownTaskException
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.JavaExec

import groovy.io.FileType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.codehaus.groovy.tools.Utilities
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaToolchainService

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@CompileStatic
class Util {
    private static final Logger LOGGER = Logging.getLogger(Util)
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
        def artifact = dep.moduleArtifacts.find {it.classifier} ?: dep.moduleArtifacts?.getAt(0)
        if(artifact) return artifact.file
        LOGGER.info "Cannot retrieve artifact $dep.name"
        return null
    }

    static void checkExecutable(String filePath) {
        checkExecutable(new File(filePath))
    }

    static void checkExecutable(File f) {
        if(!f.file) throw new GradleException("$f.absolutePath does not exist.")
        if(!f.canExecute()) throw new GradleException("$f.absolutePath is not executable.")
    }

    static Provider<RegularFile> getArchiveFile(Project project) {
        return project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar).flatMap { it.archiveFile }
    }

    @CompileDynamic
    static Provider<RegularFile> getMainDistJarFile(Project project) {
        try {
            project.tasks.named( 'shadowJar' ).flatMap { it.archiveFile }
        } catch (UnknownTaskException ignored) {
            return getArchiveFile(project)
        }
    }

    static Provider<String> getMainClass(Project project) {
        def mainClassProp = project.tasks.named( 'run', JavaExec).flatMap { it.mainClass }
        return mainClassProp.map { mainClass ->
            if(!mainClass) {
                throw new GradleException("mainClass not configured")
            }
            int pos = mainClass.lastIndexOf('/')
            if(pos < 0) return mainClass
            mainClass.substring(pos + 1)
        }
    }

    static List<String> getDefaultJvmArgs(Project project) {
        try {
            project.extensions.getByType(JavaApplication).applicationDefaultJvmArgs.toList()
        } catch (Exception e) {
            []
        }
    }

    static Provider<List<String>> getDefaultArgs(Project project) {
        try {
            project.tasks.named( 'run', JavaExec).flatMap {
                project.objects.listProperty(String).set(it.args) }
        } catch (Exception e) {
            return project.objects.listProperty(String).set([])
        }
    }

    static Provider<String> getDefaultJavaHome(Project project) {
        project.providers.provider {
            String value = System.properties['badass.runtime.java.home']
            if ( value ) return value
            value = System.getenv( 'BADASS_RUNTIME_JAVA_HOME' )
            if ( value ) return value
            value = Util.getDefaultToolchainJavaHome( project )
            if ( value ) return value
            value = System.properties['java.home']
            if ( ['javac', 'jar', 'jlink'].every { new File( "$value/bin/$it$EXEC_EXTENSION" ).file } ) return  value
            return System.getenv( 'JAVA_HOME' )
        }
    }

    static String getDefaultToolchainJavaHome(Project project) {
        try {
            def defaultToolchain = project.extensions.getByType(JavaPluginExtension)?.toolchain
            if(!defaultToolchain) return null
            JavaToolchainService service = project.extensions.getByType(JavaToolchainService)
            def launcherProvider = service?.launcherFor(defaultToolchain)
            if(!launcherProvider?.present) return null
            return launcherProvider.get()?.metadata?.installationPath?.asFile?.absolutePath
        } catch(e) {
            project.logger.warn("Cannot retrieve Java toolchain: $e")
            return null
        }
    }
}
