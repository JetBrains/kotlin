/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.v2.klib

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.v2.KlibDump
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.abi.tools.v2.ToolsV2
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
 * mergedDump.replace(newTargetDump)
 * mergedDump.saveTo(File("/path/to/merged.klib.api"))
 * ```
 */
internal class KlibDumpImpl : KlibDump {
    internal val merger: KlibAbiDumpMerger = KlibAbiDumpMerger()

    override val targets: Set<KlibTarget>
        get() = merger.targets

    override fun merge(dumpFile: File) {
        if(!dumpFile.exists()) { throw FileNotFoundException("File does not exist: ${dumpFile.absolutePath}") }
        require(dumpFile.isFile) { "Not a file: ${dumpFile.absolutePath}" }
        merger.merge(dumpFile)
    }

    override fun merge(dump: CharSequence) {
        merger.merge(dump.lineSequence().iterator())
    }

    override fun merge(other: KlibDump) {
        val intersection = targets.intersect(other.targets)
        require(intersection.isEmpty()) {
            "Cannot merge dump as this and other dumps share some targets: $intersection"
        }
        merger.merge((other as KlibDumpImpl).merger)
    }

    override fun replace(other: KlibDump) {
        remove(other.targets)
        merge(other)
    }

    override fun retain(targets: Iterable<KlibTarget>) {
        val toRemove = merger.targets.subtract(targets.toSet())
        remove(toRemove)
    }

    override fun remove(targets: Iterable<KlibTarget>) {
        targets.forEach {
            merger.remove(it)
        }
    }

    override fun copy(): KlibDump = KlibDumpImpl().also { it.merge(this) }

    override fun <A : Appendable> print(to: A): A {
        merger.dump(to)
        return to
    }

    override fun renameSingleTarget(target: KlibTarget) {
        check(merger.targets.size == 1) {
            "Can't use an explicit target name with a multi-target dump. " +
                    "new target: $target, targets in the dump: $targets"
        }
        merger.overrideTargets(setOf(target))
    }

    override fun print(file: File): File = file.apply { bufferedWriter().use { print(it) } }

    override fun inferAbiForUnsupportedTarget(previousDump: KlibDump, target: KlibTarget): KlibDump {
        // Find a set of supported targets that are closer to unsupported target in the hierarchy.
        // Note that dumps are stored using configurable name, but grouped by the canonical target name.
        val matchingTargets = findMatchingTargets(targets, target)
        // Load dumps that are a good fit for inference

        return if (matchingTargets.isNotEmpty()) {
            // case 1
            val retained = copy().also { it.retain(matchingTargets) }
            inferAbi(target, listOf(retained), previousDump)
        } else {
            val targetFromReference = previousDump.targets.firstOrNull { it.targetName == target.targetName }
            if (targetFromReference != null) {
                // case 2
                val copy = previousDump.copy()
                copy.retain(listOf(targetFromReference))
                copy
            } else if (previousDump.targets.isNotEmpty()) {
                // case 3
                KlibDumpImpl()
            } else {
                // case 4
                throw IllegalStateException(
                    "The target $target is not supported by the host compiler " +
                            "and there are no targets similar to $target to infer a dump from it."
                )
            }
        }
    }

    internal companion object {
        /**
         * Loads a dump from a textual form.
         *
         * @throws IllegalArgumentException if [dumpFile] is empty or is not a file.
         * @throws FileNotFoundException if [dumpFile] does not exist.
         */
        internal fun from(dumpFile: File): KlibDump {
            if(!dumpFile.exists()) { throw FileNotFoundException("File does not exist: ${dumpFile.absolutePath}") }
            require(dumpFile.isFile) { "Not a file: ${dumpFile.absolutePath}" }
            return KlibDumpImpl().apply { merge(dumpFile) }
        }

        internal fun from(dump: CharSequence): KlibDump {
            return KlibDumpImpl().apply { merge(dump) }
        }

        internal fun fromKlib(
            klibFile: File,
            filters: AbiFilters,
        ): KlibDump {
            val dump = buildString {
                extractAbiFromKlib(this, klibFile, filters)
            }
            return KlibDumpImpl().apply {
                merger.merge(dump.splitToSequence('\n').iterator())
            }
        }
    }
}

private fun findMatchingTargets(
    supportedTargets: Set<KlibTarget>,
    unsupportedTarget: KlibTarget
): Collection<KlibTarget> {
    var currentGroup: String? = unsupportedTarget.targetName
    while (currentGroup != null) {
        // If a current group has some supported targets, use them.
        val groupTargets = TargetHierarchy.targets(currentGroup)
        val matchingTargets = supportedTargets.filter { groupTargets.contains(it.targetName) }
        if (matchingTargets.isNotEmpty()) {
            return matchingTargets
        }
        // Otherwise, walk up the target hierarchy.
        currentGroup = TargetHierarchy.parent(currentGroup)
    }
    return emptyList()
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
 */
internal fun inferAbi(
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

    val retainedDump = KlibDumpImpl().apply {
        if (oldMergedDump != null) {
            merge(oldMergedDump)
            merger.retainTargetSpecificAbi(unsupportedTarget)
        }
    }
    val commonDump = KlibDumpImpl().apply {
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
 * It's an error to merge dumps having some targets in common.
 *
 * @throws IllegalArgumentException if this dump and [klibFile] shares same targets.
 * @throws IllegalStateException if a klib could not be loaded from [klibFile].
 * @throws FileNotFoundException if [klibFile] does not exist.
 */
internal fun KlibDump.mergeFromKlib(
    klibFile: File,
    filters: AbiFilters = AbiFilters.EMPTY
) {
    this.merge(ToolsV2.extractKlibAbi(klibFile, KlibTarget("test"), filters))
}
