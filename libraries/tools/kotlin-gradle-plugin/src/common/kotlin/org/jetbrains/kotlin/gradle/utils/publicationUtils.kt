/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.ConfigurationVariant
import org.gradle.api.provider.Provider

/**
 * Configuration of all those properties is mandatory.
 * Otherwise, Gradle versions before 8.4 may instantiate tasks registered as artifacts during dependencies overviewing without actual resolution.
 * An example of such overviewing is [[org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation]]
 * which does not require platform klibs, but klib generation tasks were instantiated at execution time (KT-71328) leading to races.
 */
private fun ConfigurablePublishArtifact.configureMandatoryProperties(
    name: String,
    type: String,
    extension: String,
    classifier: String?,
) {
    setName(name)
    setType(type)
    setExtension(extension)
    setClassifier(classifier)
}

internal fun ConfigurationVariant.registerArtifact(
    artifactProvider: Provider<*>,
    name: String,
    type: String,
    extension: String,
    classifier: String? = null,
) {
    artifact(artifactProvider) { artifact ->
        artifact.configureMandatoryProperties(name, type, extension, classifier)
    }
}

internal fun ConfigurationPublications.registerArtifact(
    artifactProvider: Provider<*>,
    name: String,
    type: String,
    extension: String,
    classifier: String? = null,
) {
    artifact(artifactProvider) { artifact ->
        artifact.configureMandatoryProperties(name, type, extension, classifier)
    }
}

internal fun ConfigurationVariant.registerKlibArtifact(
    artifactProvider: Provider<*>,
    name: String,
    classifier: String? = null,
) {
    registerArtifact(
        artifactProvider,
        name,
        "klib",
        "klib",
        classifier
    )
}

internal fun ConfigurationPublications.registerKlibArtifact(
    artifactProvider: Provider<*>,
    name: String,
    classifier: String? = null,
) {
    registerArtifact(
        artifactProvider,
        name,
        "klib",
        "klib",
        classifier
    )
}