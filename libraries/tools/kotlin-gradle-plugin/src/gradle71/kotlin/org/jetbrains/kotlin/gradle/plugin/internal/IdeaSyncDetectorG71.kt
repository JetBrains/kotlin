/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory

internal class IdeaSyncDetectorG71(private val providerFactory: ProviderFactory) : IdeaSyncDetector {
    override val isInIdeaSync = createIdeaPropertiesEvaluator().isInIdeaSync()

    override fun createIdeaPropertiesEvaluator() = object : IdeaPropertiesEvaluator() {
        // we should declare system property read for Gradle < 7.4
        override fun readSystemPropertyValue(key: String) = providerFactory.systemProperty(key).forUseAtConfigurationTime().orNull
    }

    internal class IdeaSyncDetectorVariantFactoryG71 : IdeaSyncDetector.IdeaSyncDetectorVariantFactory {
        override fun getInstance(project: Project): IdeaSyncDetector = IdeaSyncDetectorG71(project.providers)
    }
}