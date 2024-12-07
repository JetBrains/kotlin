/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.provider.ProviderFactory

/**
 * KGP's internal analog of [org.gradle.api.internal.attributes.AttributeContainerInternal.asMap]
 * Can be used to compare attributes
 */
internal fun AttributeContainer.toMap(): Map<Attribute<*>, Any?> {
    val result = mutableMapOf<Attribute<*>, Any?>()
    for (key in keySet()) {
        result[key] = getAttribute(key)
    }

    return result
}

internal fun <T : Any> HasAttributes.setAttributeProvider(
    providerFactory: ProviderFactory,
    key: Attribute<T>,
    value: () -> T
) {
    attributes.attributeProvider(
        key,
        providerFactory.provider { value() }
    )
}

/**
 * Should only be used to configure simple attributes values!
 *
 * When in doubt, prefer lazy method overload.
 */
internal fun <T : Any> HasAttributes.setAttribute(
    key: Attribute<T>,
    value: T
) {
    attributes.attribute(key, value)
}

internal fun <T : Any> HasAttributes.copyAttributeTo(
    providerFactory: ProviderFactory,
    dest: HasAttributes,
    key: Attribute<T>,
) {
    dest.setAttributeProvider(providerFactory, key) {
        attributes.getAttribute(key)
            ?: throw IllegalStateException("Failed to copy attribute. Source container is missing $key (named ${key.name}).")
    }
}

internal fun HasAttributes.copyAttributesTo(
    providerFactory: ProviderFactory,
    dest: HasAttributes,
    keys: Iterable<Attribute<*>> = attributes.keySet(),
) {
    for (key in keys) {
        copyAttributeTo(providerFactory, dest, key)
    }
}

internal inline fun <reified T> attributeOf(
    name: String
): Attribute<T> = Attribute.of(name, T::class.java)
