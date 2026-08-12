/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import java.io.InputStream
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.io.ObjectStreamClass

/**
 * An [ObjectInputStream] that only deserializes classes present in [allowedClasses] (plus primitive types and arrays
 * of allowed classes) and rejects all proxy classes. This prevents Java-deserialization gadget-chain attacks via the
 * KAPT incremental cache files read by the Kotlin Gradle Plugin (see KT-88432).
 *
 * This mirrors the whitelisting `FilteringObjectInputStream` added in the kapt-base worker process
 * (`org.jetbrains.kotlin.kapt.base.incremental`) as the fix for KT-86604 / CVE-2026-53914. Since `java.io.ObjectInputFilter`
 * is a Java 17 API and KAPT still supports Java 8, we implement our own version.
 */
internal class FilteringObjectInputStream(
    input: InputStream,
    private val allowedClasses: Set<String>,
) : ObjectInputStream(input) {
    override fun resolveClass(desc: ObjectStreamClass): Class<*> {
        if (!isWhitelistedClass(desc.name)) {
            throw InvalidClassException(desc.name, "Class is not whitelisted in KAPT incremental cache")
        }
        return super.resolveClass(desc)
    }

    override fun resolveProxyClass(interfaces: Array<out String>): Class<*> {
        throw InvalidClassException(interfaces.joinToString(), "Proxy classes are not allowed in KAPT incremental cache")
    }

    private fun isWhitelistedClass(name: String): Boolean {
        if (name in PRIMITIVE_TYPES || name in allowedClasses) {
            return true
        }

        if (!name.startsWith("[")) {
            return false
        }

        val componentType = getNonPrimitiveArrayComponentType(name) ?: return true
        return isWhitelistedClass(componentType)
    }
}

private fun getNonPrimitiveArrayComponentType(name: String): String? {
    var componentName = name
    while (componentName.startsWith("[")) {
        componentName = componentName.drop(1)
    }

    if (componentName.length == 1) {
        return null
    }

    return componentName.takeIf { it.startsWith("L") && it.endsWith(";") }?.let {
        it.substring(1, it.length - 1)
    }
}

private val PRIMITIVE_TYPES = setOf(
    "boolean",
    "byte",
    "char",
    "double",
    "float",
    "int",
    "long",
    "short",
    "void"
)
