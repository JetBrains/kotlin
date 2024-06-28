/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.provider.ProviderFactory

/**
 * While Gradle 7.4 already has [AttributeContainer.attributeProvider] method available,
 * Gradle itself has a bug in mixed attributes (eager + provider) case leading to the false-positive
 * deprecation message:
 * ```
 * Consumable configurations with identical capabilities within a project must have unique attributes, ...
 * ```
 */
internal class AttributesConfigurationHelperG74 : AttributesConfigurationHelper {
    override fun <T : Any> setAttribute(
        attributesContainer: HasAttributes,
        key: Attribute<T>,
        value: () -> T
    ) {
        attributesContainer.attributes.attribute(key, value())
    }
}

internal class AttributeConfigurationHelperVariantFactoryG74 : AttributesConfigurationHelper.AttributeConfigurationHelperVariantFactory {
    override fun getInstance(
        providerFactory: ProviderFactory
    ): AttributesConfigurationHelper = AttributesConfigurationHelperG74()
}
