/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.gradle.idea.utils.stringInterner
import org.jetbrains.kotlin.tooling.core.WeakInterner
import java.io.Serializable

fun IdeaKotlinBinaryAttributes(attributes: Map<String, String>): IdeaKotlinBinaryAttributes {
    return IdeaKotlinBinaryAttributesImpl.createAndIntern(attributes)
}

fun IdeaKotlinBinaryAttributes(): IdeaKotlinBinaryAttributes {
    return IdeaKotlinBinaryAttributesImpl.createAndIntern(emptyMap())
}

/**
 * Representing the 'Gradle attributes' associated with a given binary.
 * This can be used to identify binaries further than just their maven coordinates.
 */
@IdeaKotlinModel
sealed interface IdeaKotlinBinaryAttributes : Map<String, String>, Serializable

private class IdeaKotlinBinaryAttributesImpl private constructor(
    private val attributes: Map<String, String>,
) : IdeaKotlinBinaryAttributes, Map<String, String> by attributes {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is IdeaKotlinBinaryAttributesImpl) return false
        return this.attributes == other.attributes
    }

    override fun hashCode(): Int {
        return attributes.hashCode()
    }

    override fun toString(): String {
        return attributes.toString()
    }

    companion object {
        const val serialVersionUID = 0L

        private val interner = WeakInterner()

        internal fun createAndIntern(attributes: Map<String, String>): IdeaKotlinBinaryAttributes {
            val internedMap = attributes.map { (key, value) -> stringInterner.getOrPut(key) to stringInterner.getOrPut(value) }.toMap()
            return interner.getOrPut(IdeaKotlinBinaryAttributesImpl(internedMap))
        }
    }
}
