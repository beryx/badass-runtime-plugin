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
import org.beryx.runtime.util.Util
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal

import javax.inject.Inject

import static org.beryx.runtime.util.Util.EXEC_EXTENSION

@CompileStatic
class BaseTask extends DefaultTask {
    @Internal
    final RuntimePluginExtension extension

    BaseTask() {
        this.extension = (RuntimePluginExtension)project.extensions.getByName(RuntimePlugin.EXTENSION_NAME)
        group = 'build'
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException("Gradle overrides this method when creating the task")
    }

    @Internal
    String getJavaHomeOrDefault() {
        return extension.javaHome.present ? extension.javaHome.get() : defaultJavaHome
    }

    @Internal
    String getDefaultJavaHome() {
        def value = System.properties['badass.runtime.java.home']
        if(value) return value
        value = System.getenv('BADASS_RUNTIME_JAVA_HOME')
        if(value) return value
        value = Util.getDefaultToolchainJavaHome(project)
        if(value) return value
        value = System.properties['java.home']
        if(['javac', 'jar', 'jlink'].every { new File("$value/bin/$it$EXEC_EXTENSION").file }) return value
        return System.getenv('JAVA_HOME')
    }
}
