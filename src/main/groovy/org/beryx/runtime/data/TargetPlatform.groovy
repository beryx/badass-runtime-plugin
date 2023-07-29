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
package org.beryx.runtime.data


import org.beryx.runtime.util.JdkUtil
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

abstract class TargetPlatform implements Serializable, Named {
    private static final Logger LOGGER = Logging.getLogger(TargetPlatform.class)
    private Project project

    abstract Property<String> getJdkHome()
    abstract ListProperty<String> getOptions()

    void setProject(Project project) {
        this.project = project
    }

    void setJdkHome(Provider<String> jdkHome) {
        this.jdkHome.set(jdkHome)
    }

    void addOptions(String... opts) {
        this.options.addAll(opts)
    }

    Provider<String> jdkDownload(String downloadUrl, Closure downloadConfig = null) {
        def options = new JdkUtil.JdkDownloadOptions(project, name, downloadUrl)
        if(downloadConfig) {
            downloadConfig.delegate = options
            downloadConfig(options)
        }
        return new DefaultProvider<String>({
            def relativePathToHome = JdkUtil.downloadFrom(downloadUrl, options)
            def pathToHome = "$options.downloadDir/$relativePathToHome"
            LOGGER.info("Home of downloaded JDK distribution: $pathToHome")
            return pathToHome as String
        })
    }
}
