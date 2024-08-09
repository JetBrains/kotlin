/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.provider.ProviderFactory

internal class AttributesConfigurationHelperG71 : AttributesConfigurationHelper {
    override fun <T : Any> setAttribute(
        attributesContainer: HasAttributes,
        key: Attribute<T>,
        value: () -> T
    ) {
        attributesContainer.attributes.attribute(key, value())
    }
}

internal class AttributeConfigurationHelperVariantFactoryG71 : AttributesConfigurationHelper.AttributeConfigurationHelperVariantFactory {
    override fun getInstance(
        providerFactory: ProviderFactory
    ): AttributesConfigurationHelper = AttributesConfigurationHelperG71()
}
