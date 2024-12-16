/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api.v3

import java.io.File

/**
 * Represents ABI dump and allows manipulating it.
 *
 * This type is mutable.
 *
 * @since 2.1.20
 */
public interface AbiDump {
    /**
     * Set of all targets for which this dump contains declarations.
     */
    public val targets: List<DumpTarget>

    /**
     * Merges [other] dump with this one.
     *
     * It's also an error to merge dumps having some targets in common.
     *
     * The operation does not modify [other].
     *
     * @throws IllegalArgumentException if this dump and [other] shares same targets.
     */
    public fun merge(other: AbiDump)

    /**
     * Create a copy of this dump.
     */
    public fun copy(): AbiDump

    /**
     * Write ABI declarations in a text form into specified [targetFile] file.
     */
    public fun print(targetFile: File)

    /**
     * Write ABI declarations in a text form into specified [appender].
     */
    public fun print(appender: Appendable)
}

/**
 * Representation of Kotlin Target in ABI dumps.
 *
 * @since 2.1.20
 */
public class DumpTarget(
    /**
     * Name of Kotlin Target.
     * Can be overridden during configuration
     */
    public val name: String,

    /**
     * The type that defines the target platform.
     */
    public val type: KotlinTargetType,
)

/**
 * Description of platform type for some Kotlin Target.
 *
 * It is represented as a sequence of nested groups that include this target in a specific version of the compiler.
 * Groups are divided by `/` symbol, e.g. `klib-abi/native/linux/linuxX64`.
 *
 * The names of the groups and their nesting may change between different versions of the compiler,
 * so to support backward compatibility, they are specified as a full string.
 *
 * @since 2.1.20
 */
public class KotlinTargetType(private val name: String) {
    /**
     * Get string represented of type in the form of nested groups separated by `/`.
     */
    public fun asString(): String {
        return name
    }

    /**
     *  Gettype as nested groups.
     *
     *  The root group always exists and has index `0`, as the index increases, the nesting of groups increases.
     */
    public fun asSegments(): List<String> {
        return name.split("/")
    }
}
