/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi
import java.io.Serializable

inline fun <reified T : Any> KotlinExternalModelId(disambiguationName: String? = null): KotlinExternalModelId<T> {
    return KotlinExternalModelId(KotlinExternalModelType(), disambiguationName)
}

class KotlinExternalModelId<T : Any> @PublishedApi internal constructor(
    private val type: KotlinExternalModelType<T>,
    private val disambiguationName: String? = null
) : Serializable {
    override fun toString(): String {
        return if (disambiguationName == null) type.toString()
        else "$disambiguationName:$type"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KotlinExternalModelId<*>
        if (type != other.type) return false
        if (disambiguationName != other.disambiguationName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (disambiguationName?.hashCode() ?: 0)
        return result
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
