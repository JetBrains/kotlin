/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

import kotlin.reflect.KVisibility

private const val METADATA_KIND_CLASS = 1

// Checked once, so that hosts without kotlin-reflect don't get a KotlinReflectionNotSupportedError thrown for every class.
private val isKotlinReflectionAvailable: Boolean by lazy {
    runCatching { Metadata::class.visibility }.isSuccess
}

/**
 * The default [Jsr223HostConfigurationKeys.hasPublicKotlinVisibility] implementation. Returns true for a non-Kotlin class.
 * For a Kotlin class, returns whether its [kotlin.reflect.KClass.visibility] is public, or false if `kotlin-reflect` is not
 * available or fails to load the class, so the caller falls back to a supertype instead of emitting a reference that may
 * not compile. Kotlin classes other than regular classes (e.g. lambdas or file facades) can't be referenced by name, so
 * false is returned for them too.
 */
internal fun hasPublicKotlinVisibilityViaReflection(jClass: Class<*>): Boolean {
    val metadata = jClass.getAnnotation(Metadata::class.java) ?: return true
    if (metadata.kind != METADATA_KIND_CLASS || !isKotlinReflectionAvailable) return false
    return runCatching { jClass.kotlin.visibility == KVisibility.PUBLIC }.getOrDefault(false)
}
