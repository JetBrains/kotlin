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
 *
 * When artifact is exported as secondary variant [name], [extension] and [classifier] are not used,
 * so setting it to some "default" value is acceptable. But one has to be careful when setting those
 * on publishable artifacts. Check [ConfigurationPublications.registerArtifact]
 */
private fun ConfigurablePublishArtifact.configureMandatoryProperties(
    name: String = "default-name",
    type: String = "default-type",
    extension: String = "default-extension",
    classifier: String?,
) {
    this.name = name
    this.type = type
    this.extension = extension
    this.classifier = classifier
}

internal fun ConfigurationVariant.registerArtifact(
    artifactProvider: Provider<*>,
    name: String = "default-name",
    type: String = "default-type",
    extension: String = "default-extension",
    classifier: String? = null,
    configure: ConfigurablePublishArtifact.() -> Unit = {},
) {
    artifact(artifactProvider) { artifact ->
        artifact.configureMandatoryProperties(name, type, extension, classifier)
        artifact.configure()
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