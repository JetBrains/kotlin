/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api.klib

import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.kotlin.ir.backend.js.MainModule
import java.io.File
import java.io.FileNotFoundException

/**
 * Represents KLib ABI dump and allows manipulating it.
 *
 * Usual [KlibDump] workflows consists of loading, updating and writing a dump back.
 *
 * **Creating a textual dump from a klib**
 * ```kotlin
 * val dump = KlibDump.fromKlib(File("/path/to/library.klib"))
 * dump.saveTo(File("/path/to/dump.klib.api"))
 * ```
 *
 * **Loading a dump**
 * ```kotlin
 * val dump = KlibDump.from(File("/path/to/dump.klib.api"))
 * ```
 *
 * **Merging multiple dumps into a new merged dump**
 * ```kotlin
 * val klibs = listOf(File("/path/to/library-linuxX64.klib"), File("/path/to/library-linuxArm64.klib"), ...)
 * val mergedDump = KlibDump()
 * klibs.forEach { mergedDump.mergeFromKlib(it) }
 * mergedDump.saveTo(File("/path/to/merged.klib.api"))
 * ```
 *
 * **Updating an existing merged dump**
 * ```kotlin
 * val mergedDump = KlibDump.from(File("/path/to/merged.klib.api"))
 * val newTargetDump = KlibDump.fromKlib(File("/path/to/library-linuxX64.klib"))
 * mergedDump.remove(newTargetDump.targets)
 * mergedDump.merge(newTargetDump)
 * mergedDump.saveTo(File("/path/to/merged.klib.api"))
 * ```
 */
@ExperimentalBCVApi
public class KlibDump {
    internal val merger: KlibAbiDumpMerger = KlibAbiDumpMerger()

    /**
     * Set of all targets for which this dump contains declarations.
     *
     * @sample samples.KlibDumpSamples.extractTargets
     */
    public val targets: Set<KlibTarget>
        get() = merger.targets


    /**
     * Loads a textual KLib dump and merges it into this dump.
     *
     * If a dump contains only a single target, it's possible to specify a custom configurable target name.
     * Please refer to [KlibTarget.configurableName] for more details on the meaning of that name.
     *
     * By default, [configurableTargetName] is null and information about a target will be taken directly from
     * the loaded dump.
     *
     * It's an error to specify non-null [configurableTargetName] for a dump containing multiple targets.
     * It's also an error to merge dumps having some targets in common.
     *
     * @throws IllegalArgumentException if this dump and [dumpFile] shares same targets.
     * @throws IllegalArgumentException if [dumpFile] contains multiple targets
     * and [configurableTargetName] is not null.
     * @throws IllegalArgumentException if [dumpFile] is not a file.
     * @throws FileNotFoundException if [dumpFile] does not exist.
     *
     * @sample samples.KlibDumpSamples.mergeDumps
     */
    public fun merge(dumpFile: File, configurableTargetName: String? = null) {
        if(!dumpFile.exists()) { throw FileNotFoundException("File does not exist: ${dumpFile.absolutePath}") }
        require(dumpFile.isFile) { "Not a file: ${dumpFile.absolutePath}" }
        merger.merge(dumpFile, configurableTargetName)
    }

    /**
     * Merges [other] dump with this one.
     *
     * It's also an error to merge dumps having some targets in common.
     *
     * The operation does not modify [other].
     *
     * @throws IllegalArgumentException if this dump and [other] shares same targets.
     *
     * @sample samples.KlibDumpSamples.mergeDumpObjects
     */
    public fun merge(other: KlibDump) {
        val intersection = targets.intersect(other.targets)
        require(intersection.isEmpty()) {
            "Cannot merge dump as this and other dumps share some targets: $intersection"
        }
        merger.merge(other.merger)
    }

    /**
     * Removes all declarations that do not belong to specified targets and removes these targets from the dump.
     *
     * All targets in the [targets] collection not contained within this dump will be ignored.
     *
     * @sample samples.KlibDumpSamples.extractTargets
     */
    public fun retain(targets: Iterable<KlibTarget>) {
        val toRemove = merger.targets.subtract(targets.toSet())
        remove(toRemove)
    }

    /**
     * Remove all declarations that do belong to specified targets and remove these targets from the dump.
     *
     * All targets in the [targets] collection not contained within this dump will be ignored.
     *
     * @sample samples.KlibDumpSamples.mergeDumpObjects
     */
    public fun remove(targets: Iterable<KlibTarget>) {
        targets.forEach {
            merger.remove(it)
        }
    }

    /**
     * Creates a copy of this dump.
     */
    public fun copy(): KlibDump = KlibDump().also { it.merge(this) }

    /**
     * Serializes the dump and writes it to [to].
     *
     * @sample samples.KlibDumpSamples.mergeDumps
     */
    public fun saveTo(to: Appendable) {
        merger.dump(to)
    }

    public companion object {
        /**
         * Loads a dump from a textual form.
         *
         * If a dump contains only a single target, it's possible to specify a custom configurable target name.
         * Please refer to [KlibTarget.configurableName] for more details on the meaning of that name.
         *
         * By default, [configurableTargetName] is null and information about a target will be taken directly from
         * the loaded dump.
         *
         * It's an error to specify non-null [configurableTargetName] for a dump containing multiple targets.
         *
         * @throws IllegalArgumentException if [dumpFile] contains multiple targets
         * and [configurableTargetName] is not null.
         * @throws IllegalArgumentException if [dumpFile] is empty.
         * @throws IllegalArgumentException if [dumpFile] is not a file.
         * @throws FileNotFoundException if [dumpFile] does not exist.
         *
         * @sample samples.KlibDumpSamples.mergeDumpObjects
         */
        public fun from(dumpFile: File, configurableTargetName: String? = null): KlibDump {
            if(!dumpFile.exists()) { throw FileNotFoundException("File does not exist: ${dumpFile.absolutePath}") }
            require(dumpFile.isFile) { "Not a file: ${dumpFile.absolutePath}" }
            return KlibDump().apply { merge(dumpFile, configurableTargetName) }
        }

        /**
         * Dumps a public ABI of a klib represented by [klibFile] using [filters]
         * and returns a [KlibDump] representing it.
         *
         * To control which declarations are dumped, [filters] could be used. By default, no filters will be applied.
         *
         * If a klib contains only a single target, it's possible to specify a custom configurable target name.
         * Please refer to [KlibTarget.configurableName] for more details on the meaning of that name.
         *
         * By default, [configurableTargetName] is null and information about a target will be taken directly from
         * the klib.
         *
         * It's an error to specify non-null [configurableTargetName] for a klib containing multiple targets.
         *
         * @throws IllegalArgumentException if [klibFile] contains multiple targets
         * and [configurableTargetName] is not null.
         * @throws IllegalStateException if a klib could not be loaded from [klibFile].
         * @throws FileNotFoundException if [klibFile] does not exist.
         */
        public fun fromKlib(
            klibFile: File,
            configurableTargetName: String? = null,
            filters: KlibDumpFilters = KlibDumpFilters.DEFAULT
        ): KlibDump {
            val dump = buildString {
                dumpTo(this, klibFile, filters)
            }
            return KlibDump().apply {
                merger.merge(dump.splitToSequence('\n').iterator(), configurableTargetName)
            }
        }
    }
}

/**
 * Infer a possible public ABI for [unsupportedTarget] as an ABI common across all [supportedTargetDumps].
 * If there's an [oldMergedDump] consisting of declarations of multiple targets, including [unsupportedTarget],
 * a portion of that dump specific to the [unsupportedTarget] will be extracted and merged to the common ABI
 * build from [supportedTargetDumps].
 *
 * Returned dump contains only declarations for [unsupportedTarget].
 *
 * The function aimed to facilitate ABI dumps generation for targets that are not supported by a host compiler.
 * In practice, it means generating dumps for Apple targets on non-Apple hosts.
 *
 * @throws IllegalArgumentException when one of [supportedTargetDumps] contains [unsupportedTarget]
 * @throws IllegalArgumentException when [supportedTargetDumps] are empty and [oldMergedDump] is null
 *
 * @sample samples.KlibDumpSamples.inferDump
 */
@ExperimentalBCVApi
public fun inferAbi(
    unsupportedTarget: KlibTarget,
    supportedTargetDumps: Iterable<KlibDump>,
    oldMergedDump: KlibDump? = null
): KlibDump {
    require(supportedTargetDumps.iterator().hasNext() || oldMergedDump != null) {
        "Can't infer a dump without any dumps provided (supportedTargetDumps is empty, oldMergedDump is null)"
    }
    supportedTargetDumps.asSequence().flatMap { it.targets }.toSet().also {
        require(!it.contains(unsupportedTarget)) {
            "Supported target dumps already contains unsupportedTarget=$unsupportedTarget"
        }
    }

    val retainedDump = KlibDump().apply {
        if (oldMergedDump != null) {
            merge(oldMergedDump)
            merger.retainTargetSpecificAbi(unsupportedTarget)
        }
    }
    val commonDump = KlibDump().apply {
        supportedTargetDumps.forEach {
            merge(it)
        }
        merger.retainCommonAbi()
    }
    commonDump.merge(retainedDump)
    commonDump.merger.overrideTargets(setOf(unsupportedTarget))
    return commonDump
}

/**
 * Dumps a public ABI of a klib represented by [klibFile] using [filters] and merges it into this dump.
 *
 * To control which declarations are dumped, [filters] could be used. By default, no filters will be applied.
 *
 * If a klib contains only a single target, it's possible to specify a custom configurable target name.
 * Please refer to [KlibTarget.configurableName] for more details on the meaning of that name.
 *
 * By default, [configurableTargetName] is null and information about a target will be taken directly from
 * the klib.
 *
 * It's an error to specify non-null [configurableTargetName] for a klib containing multiple targets.
 * It's also an error to merge dumps having some targets in common.
 *
 * @throws IllegalArgumentException if this dump and [klibFile] shares same targets.
 * @throws IllegalArgumentException if [klibFile] contains multiple targets
 * and [configurableTargetName] is not null.
 * @throws IllegalStateException if a klib could not be loaded from [klibFile].
 * @throws FileNotFoundException if [klibFile] does not exist.
 */
@ExperimentalBCVApi
public fun KlibDump.mergeFromKlib(
    klibFile: File, configurableTargetName: String? = null,
    filters: KlibDumpFilters = KlibDumpFilters.DEFAULT
) {
    this.merge(KlibDump.fromKlib(klibFile, configurableTargetName, filters))
}

/**
 * Serializes the dump and writes it to [file].
 */
@ExperimentalBCVApi
public fun KlibDump.saveTo(file: File): Unit = file.bufferedWriter().use { saveTo(it) }
