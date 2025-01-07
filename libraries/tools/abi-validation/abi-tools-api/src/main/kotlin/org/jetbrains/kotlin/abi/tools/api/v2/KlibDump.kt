/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api.v2

import java.io.File

/**
 * Represents a KLib ABI dump and allows manipulating it.
 *
 * This type is mutable.
 *
 * Usual [KlibDump] workflows consist of loading, updating and writing a dump back.
 *
 * **Creating a textual dump from a klib**
 * ```kotlin
 * val dump = abiTools.v2.extractKlibAbi(File("/path/to/library.klib"), KlibTarget("linuxX64"))
 * dump.print(File("/path/to/dump.klib.api"))
 * ```
 *
 * **Loading a dump**
 * ```kotlin
 * val dump = abiTools.v2.loadKlibDump(File("/path/to/dump.klib.api"))
 * ```
 *
 * **Merging multiple dumps into a new merged dump**
 * ```kotlin
 * val klibs = listOf(File("/path/to/library-linuxX64.klib") to KlibTarget("linuxX64"), File("/path/to/library-linuxArm64.klib") to KlibTarget("linuxArm64"), ...)
 * val mergedDump = abiTools.v2.createKlibDump()
 * klibs.forEach { mergedDump.merge(abiTools.v2.parseKlib(it.first, it.second)) }
 * mergedDump.print(File("/path/to/merged.klib.api"))
 * ```
 *
 * **Updating an existing merged dump**
 * ```kotlin
 * val mergedDump = abiTools.v2.loadKlibDump(File("/path/to/merged.klib.api"))
 * val newTargetDump = abiTools.v2.extractKlibAbi(File("/path/to/library-linuxX64.klib"), KlibTarget("linuxX64"))
 * mergedDump.replace(newTargetDump)
 * mergedDump.print(File("/path/to/merged.klib.api"))
 * ```
 *
 * @since 2.1.20
 */
public interface KlibDump {
    /**
     * Set of all targets for which this dump contains declarations.
     */
    public val targets: Set<KlibTarget>

    /**
     * Loads a textual KLib dump and merges it into this dump.
     *
     * It's an error to merge dumps having some targets in common.
     *
     * @throws IllegalArgumentException if this dump and [dumpFile] shares same targets.
     * @throws IllegalArgumentException if [dumpFile] is not a file or is empty.
     * @throws java.io.FileNotFoundException if [dumpFile] does not exist.
     */
    public fun merge(dumpFile: File)

    /**
     * Reads a textual KLib dump from the [dump] char sequence and merges it into this dump.
     *
     * It's also an error to merge dumps having some targets in common.
     *
     * @throws IllegalArgumentException if this dump and the provided [dump] shares same targets.
     * @throws IllegalArgumentException if the provided [dump] is empty.
     */
    public fun merge(dump: CharSequence)

    /**
     * Merges [other] dump with this one.
     *
     * It's also an error to merge dumps having some targets in common.
     *
     * The operation does not modify [other].
     *
     * @throws IllegalArgumentException if this dump and [other] shares same targets.
     */
    public fun merge(other: KlibDump)

    /**
     * Removes the targets from this dump that are contained in the [other] targets set and all their declarations.
     * Then merges the [other] dump with this one.
     *
     * The operation does not modify [other].
     */
    public fun replace(other: KlibDump)

    /**
     * Removes all declarations that do not belong to specified targets and removes these targets from the dump.
     *
     * All targets in the [targets] collection not contained within this dump will be ignored.
     */
    public fun retain(targets: Iterable<KlibTarget>)

    /**
     * Remove all declarations that do belong to specified targets and remove these targets from the dump.
     *
     * All targets in the [targets] collection not contained within this dump will be ignored.
     */
    public fun remove(targets: Iterable<KlibTarget>)

    /**
     * Change target identifier in this dump if it contains only single target.
     *
     * @throws IllegalStateException if dump contains multiple targets
     */
    public fun renameSingleTarget(target: KlibTarget)

    /**
     * Creates a copy of this dump.
     */
    public fun copy(): KlibDump

    /**
     * Serializes the dump and prints it to [to].
     *
     * @return the target [to] where the dump was written.
     */
    public fun <A : Appendable> print(to: A): A

    /**
     * Serializes the dump and prints it to [file].
     *
     * @return the target [file].
     */
    public fun print(file: File): File

    /**
     * Tries to derive the ABI for the specified [target] from the actual this dump in and the previous dump in [previousDump].
     *
     * Calling this function makes sense only when the current dump does not contain some targets that
     * are present in the [previousDump] due to the fact that compilation of these targets is not supported on the current host.
     *
     * Depending on the values passed, the function works differently:
     * - if this dump contains any targets: common declarations for closest targets* present in them all merged with
     * declarations from [previousDump] for [target] (if present) but which are not found in any of the other targets from the [previousDump]
     * - if this dump is empty and [target] is present in [previousDump]: all declaration for [target] from [previousDump]
     * - if this dump is empty and [target] is not present in not-empty [previousDump]: empty dump
     * - if this dump and [previousDump] are empty: throw [IllegalStateException] because there is no source of information from
     * where we can infer ABI for the [target].
     *
     *
     * *Closest targets is a related targets that have a common ancestor with [target].
     * Because in the end, all goals have a common root ancestor, so the targets with the closest to [target] are returned.
     */
    public fun inferAbiForUnsupportedTarget(previousDump: KlibDump, target: KlibTarget): KlibDump
}