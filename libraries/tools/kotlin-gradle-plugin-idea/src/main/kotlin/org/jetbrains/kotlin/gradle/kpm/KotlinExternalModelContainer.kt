/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi
import java.io.Serializable

sealed class KotlinExternalModelContainer : Serializable {
    abstract val ids: Set<KotlinExternalModelId<*>>
    abstract operator fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean
    abstract operator fun <T : Any> get(key: KotlinExternalModelKey<T>): T?
    fun <T : Any> getOrThrow(key: KotlinExternalModelKey<T>): T = get(key)
        ?: throw NoSuchElementException("Missing external model for ${key.id}")

    fun isEmpty(): Boolean = ids.isEmpty()
    fun isNotEmpty(): Boolean = ids.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (other !is KotlinExternalModelContainer) return false
        if (this.isEmpty() && other.isEmpty()) return true
        return super.equals(other)
    }

    override fun hashCode(): Int {
        if (this.isEmpty()) return 0
        return super.hashCode()
    }

    @InternalKotlinGradlePluginApi
    companion object {
        fun mutable(): KotlinMutableExternalModelContainer = KotlinMutableExternalModelContainerImpl()
    }

    object Empty : KotlinExternalModelContainer() {
        override val ids: Set<KotlinExternalModelId<*>> = emptySet()
        override fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean = false
        override fun <T : Any> get(key: KotlinExternalModelKey<T>): T? = null


        private const val serialVersionUID = 0L
    }

    override fun toString(): String {
        return "KotlinExternalModelContainer(${ids.joinToString("; ", "[", "]")}"
    }
}

sealed class KotlinMutableExternalModelContainer : KotlinExternalModelContainer() {
    abstract operator fun <T : Any> set(key: KotlinExternalModelKey<T>, value: T)
}
