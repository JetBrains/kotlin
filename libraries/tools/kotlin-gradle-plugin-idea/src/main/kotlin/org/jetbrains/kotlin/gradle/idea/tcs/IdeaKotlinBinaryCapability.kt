/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import java.io.Serializable

/**
 * Several variants can be published under a given set of maven coordinates (group, module, version),
 * Capabilities can classify those variants in a way that e.g. two variants with the same maven coordinates
 * can be resolved next to each other.
 *
 * A classical example for this would be resolving the 'main' (aka 'production') variant alongside
 * the corresponding "testFixtures".
 *
 * See:
 *  - https://docs.gradle.org/current/userguide/feature_variants.html
 *  - https://docs.gradle.org/current/userguide/component_capabilities.html
 * @since 1.9.20
 */
@IdeaKotlinModel
class IdeaKotlinBinaryCapability(
    val group: String,
    val name: String,
    val version: String?,
) : Serializable {
    internal companion object {
        const val serialVersionUID = 0L
    }

    override fun toString(): String {
        return buildString {
            append("$group:$name")
            if (version != null) append(":$version")
        }
    }

    fun copy(
        group: String = this.group,
        name: String = this.name,
        version: String? = this.version,
    ): IdeaKotlinBinaryCapability {
        return IdeaKotlinBinaryCapability(group = group, name = name, version = version)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IdeaKotlinBinaryCapability) return false
        if (group != other.group) return false
        if (name != other.name) return false
        if (version != other.version) return false
        return true
    }

    override fun hashCode(): Int {
        var result = group.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        return result
    }
}
