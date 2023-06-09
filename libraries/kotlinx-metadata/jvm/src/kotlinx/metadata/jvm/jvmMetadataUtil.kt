/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmMetadataUtil")

package kotlinx.metadata.jvm

import kotlinx.metadata.ClassName
import kotlinx.metadata.isLocalClassName

/**
 * Converts [this] to a JVM internal name of the class, where package names are separated by '/', and class names are separated by '$',
 * for example: `"org/foo/bar/Baz.Nested"` -> `"org/foo/bar/Baz$Nested"`
 */
fun ClassName.toJvmInternalName(): String =
    if (this.isLocalClassName()) substring(1)
    else replace('.', '$')

// Deprecated since 0.6.1, should be error in 0.7.0+
@Deprecated(
    "Renamed to toJvmInternalName() to avoid confusion with String properties",
    ReplaceWith("toJvmInternalName()"),
    level = DeprecationLevel.WARNING
)
val ClassName.jvmInternalName: String get() = toJvmInternalName()

/**
 * Helper function to instantiate [Metadata].
 * Contrary to a direct constructor call, this one accepts nullable parameters to substitute nulls with default values.
 * Also, this one does not accept [Metadata.bytecodeVersion] as it is deprecated.
 */
fun Metadata(
    kind: Int? = null,
    metadataVersion: IntArray? = null,
    data1: Array<String>? = null,
    data2: Array<String>? = null,
    extraString: String? = null,
    packageName: String? = null,
    extraInt: Int? = null
): Metadata = Metadata(
    kind ?: 1,
    metadataVersion ?: intArrayOf(),
    intArrayOf(1, 0, 3),
    data1 ?: emptyArray(),
    data2 ?: emptyArray(),
    extraString ?: "",
    packageName ?: "",
    extraInt ?: 0
)
