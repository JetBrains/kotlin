/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.nodejs.protractor

import com.intellij.execution.configuration.ConfigurationFactoryEx
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.project.Project
import icons.JavaScriptLanguageIcons

class KotlinProtractorConfigurationType : ConfigurationTypeBase(
        "KotlinJavaScriptTestRunnerProtractor",
        "Protractor (Kotlin)",
        NAME,
        JavaScriptLanguageIcons.Protractor.Protractor
) {
    init {
        addFactory(object : ConfigurationFactoryEx<KotlinProtractorRunConfiguration>(this) {
            override fun createTemplateConfiguration(project: Project) = KotlinProtractorRunConfiguration(project, this, NAME)

            override fun isConfigurationSingletonByDefault() = true

            override fun canConfigurationBeSingleton() = false

            override fun onNewConfigurationCreated(configuration: KotlinProtractorRunConfiguration) = configuration.initialize()
        })
    }

    companion object {
        private val NAME = "Protractor"

        fun getInstance() = ConfigurationTypeUtil.findConfigurationType(KotlinProtractorConfigurationType::class.java)

        val factory: ConfigurationFactory
            get() = getInstance().configurationFactories.first()
    }
}
