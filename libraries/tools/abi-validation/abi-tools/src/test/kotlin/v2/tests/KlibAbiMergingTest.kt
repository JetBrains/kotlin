/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.legacy

import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.abi.tools.v2.klib.DeclarationType
import org.jetbrains.kotlin.abi.tools.v2.klib.KlibAbiDumpMerger
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*
import kotlin.test.Test

class KlibAbiMergingTest {
    @JvmField
    @Rule
    val tempDir = TemporaryFolder()

    private fun file(name: String): File {
        val res = KlibAbiMergingTest::class.java.getResourceAsStream(name)
            ?: throw IllegalStateException("Resource not found: $name")
        val tempFile = File(tempDir.root, UUID.randomUUID().toString())
        Files.copy(res, tempFile.toPath())
        return tempFile
    }

    private fun lines(name: String): Sequence<String> {
        val res = KlibAbiMergingTest::class.java.getResourceAsStream(name)
            ?: throw IllegalStateException("Resource not found: $name")
        return res.bufferedReader().lineSequence()
    }

    private fun dumpToFile(klib: KlibAbiDumpMerger): File {
        val file = tempDir.newFile()
        FileWriter(file).use {
            klib.dump(it)
        }
        return file
    }

    @Test
    fun identicalDumpFiles() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/identical/dump_macos_arm64.abi"))
        klib.merge(file("/merge/identical/dump_linux_x64.abi"))
        val merged = dumpToFile(klib)

        assertContentEquals(
            lines("/merge/identical/merged.abi"),
            Files.readAllLines(merged.toPath()).asSequence()
        )
    }

    @Test
    fun identicalDumpFilesWithAliases() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/identical/dump_macos_arm64.abi"))
        klib.merge(file("/merge/identical/dump_linux_x64.abi"))
        val merged = dumpToFile(klib)

        // there are no groups other than "all", so no aliases will be added
        assertContentEquals(
            lines("/merge/identical/merged.abi"),
            Files.readAllLines(merged.toPath()).asSequence()
        )
    }

    @Test
    fun divergingDumpFiles() {
        val targets = mutableListOf("androidNativeArm64", "linuxArm64", "linuxX64", "tvosX64")
        val random = Random(42)
        for (i in 0 until 10) {
            val klib = KlibAbiDumpMerger()
            targets.shuffle(random)
            targets.forEach {
                klib.merge(file("/merge/diverging/$it.api"))
            }
            val merged = dumpToFile(klib)
            assertContentEquals(
                lines("/merge/diverging/merged.abi"),
                Files.readAllLines(merged.toPath()).asSequence(),
                merged.readText()
            )
        }
    }

    @Test
    fun divergingDumpFilesWithAliases() {
        val random = Random(42)
        for (i in 0 until 10) {
            val klib = KlibAbiDumpMerger()
            val targets = mutableListOf("androidNativeArm64", "linuxArm64", "linuxX64", "tvosX64")
            targets.shuffle(random)
            targets.forEach {
                klib.merge(file("/merge/diverging/$it.api"))
            }
            val merged = dumpToFile(klib)
            assertContentEquals(
                lines("/merge/diverging/merged_with_aliases.abi"),
                Files.readAllLines(merged.toPath()).asSequence()
            )
        }
    }

    @Test
    fun mergeDumpsWithDivergedHeaders() {
        val klib = KlibAbiDumpMerger()
        klib.merge(loadAndRename(file("/merge/header-mismatch/v1.abi"), "linuxArm64"))

        assertFailsWith<IllegalArgumentException> {
            klib.merge(loadAndRename(file("/merge/header-mismatch/v2.abi"), "linuxX64"))
        }
    }

    @Test
    fun overwriteAll() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/diverging/merged.abi"))

        val targets = listOf("androidNativeArm64", "linuxArm64", "linuxX64", "tvosX64")
        targets.forEach { target ->
            klib.remove(KlibTarget(target))
            klib.merge(file("/merge/diverging/$target.api"))
        }

        val merged = dumpToFile(klib)

        assertContentEquals(
            lines("/merge/diverging/merged.abi"),
            Files.readAllLines(merged.toPath()).asSequence()
        )
    }

    @Test
    fun read() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/idempotent/bcv-klib-test.abi"))

        val written = dumpToFile(klib)
        assertContentEquals(
            lines("/merge/idempotent/bcv-klib-test.abi"),
            Files.readAllLines(written.toPath()).asSequence()
        )
    }

    @Test
    fun readDeclarationWithNarrowerChildrenDeclarations() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/parseNarrowChildrenDecls/merged.abi"))

        klib.remove(KlibTarget("linuxArm64"))
        val written1 = dumpToFile(klib)
        assertContentEquals(
            lines("/merge/parseNarrowChildrenDecls/withoutLinuxArm64.abi"),
            Files.readAllLines(written1.toPath()).asSequence()
        )

        klib.remove(KlibTarget("linuxX64"))
        val written2 = dumpToFile(klib)
        assertContentEquals(
            lines("/merge/parseNarrowChildrenDecls/withoutLinuxAll.abi"),
            Files.readAllLines(written2.toPath()).asSequence()
        )
    }

    @Test
    fun guessAbi() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/guess/merged.api"))
        klib.retainTargetSpecificAbi(KlibTarget("linuxArm64"))

        val retainedLinuxAbiDump = dumpToFile(klib)
        assertContentEquals(
            lines("/merge/guess/linuxArm64Specific.api"),
            Files.readAllLines(retainedLinuxAbiDump.toPath()).asSequence()
        )

        val commonAbi = KlibAbiDumpMerger()
        commonAbi.merge(file("/merge/guess/merged.api"))
        commonAbi.remove(KlibTarget("linuxArm64"))
        commonAbi.retainCommonAbi()

        val commonAbiDump = dumpToFile(commonAbi)
        assertContentEquals(
            lines("/merge/guess/common.api"),
            Files.readAllLines(commonAbiDump.toPath()).asSequence()
        )

        commonAbi.mergeTargetSpecific(klib)
        commonAbi.overrideTargets(setOf(KlibTarget("linuxArm64")))

        val guessedAbiDump = dumpToFile(commonAbi)
        assertContentEquals(
            lines("/merge/guess/guessed.api"),
            Files.readAllLines(guessedAbiDump.toPath()).asSequence()
        )
    }

    @Test
    fun loadInvalidFile() {
        assertFails {
            KlibAbiDumpMerger().merge(file("/merge/illegalFiles/emptyFile.txt"))
        }

        assertFails {
            KlibAbiDumpMerger().merge(file("/merge/illegalFiles/nonDumpFile.txt"))
        }
    }

    @Test
    fun webTargets() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/webTargets/js.abi"))
        klib.merge(loadAndRename(file("/merge/webTargets/wasmWasi.abi"), KlibTarget("wasmWasi")))
        klib.merge(loadAndRename(file("/merge/webTargets/wasmJs.abi"), KlibTarget("wasmJs")))

        val merged = dumpToFile(klib)

        assertContentEquals(
            lines("/merge/webTargets/merged.abi"),
            Files.readAllLines(merged.toPath()).asSequence()
        )
    }

    @Test
    fun explicitWasmTargets() {
        val klib = KlibAbiDumpMerger()
        klib.merge(file("/merge/explicitWasmTargets/wasmWasi.abi"))
        klib.merge(file("/merge/explicitWasmTargets/wasmJs.abi"))

        val merged = dumpToFile(klib)

        assertContentEquals(
            lines("/merge/explicitWasmTargets/merged.abi"),
            Files.readAllLines(merged.toPath()).asSequence()
        )
    }

    @Test
    fun renameWasmTargetHavingNameInManifest() {
        val klib = KlibAbiDumpMerger()
        klib.merge(loadAndRename(file("/merge/explicitWasmTargets/wasmWasi.abi"), "wasm"))
        assertEquals(setOf(KlibTarget.parse("wasmWasi.wasm")), klib.targets)
    }

    @Test
    fun wasmDumpWithMultipleTargets() {
        val klib = KlibAbiDumpMerger()
        assertFailsWith<IllegalArgumentException>{
            klib.merge(loadAndRename(file("/merge/explicitWasmTargets/wasmMulti.abi"), "wasm"))
        }

        klib.merge(file("/merge/explicitWasmTargets/wasmMulti.abi"))
        assertContentEquals(
            lines("/merge/explicitWasmTargets/merged.abi"),
            Files.readAllLines(dumpToFile(klib).toPath()).asSequence()
        )
    }

    @Test
    fun unqualifiedWasmTarget() {
        // currently, there's no way to distinguish wasmWasi from wasmJs
        // so we call this target 'wasm', and we expect that in the future it will be renamed
        val dump = KlibAbiDumpMerger()
        dump.merge(file("/merge/webTargets/wasmWasi.abi"))
        assertEquals(KlibTarget("wasm"), dump.targets.single())
    }

    @Test
    fun intersectingTargets() {
        val dump = KlibAbiDumpMerger().apply {
            merge(file("/merge/diverging/merged.abi"))
        }
        assertFailsWith<IllegalStateException> {
            dump.merge(file("/merge/diverging/linuxArm64.api"))
        }
        // but here, we're loading a dump for different target (configuredName changed)
        dump.merge(loadAndRename(file("/merge/diverging/linuxArm64.api"), "custom"))
    }

    @Test
    fun customTargetNames() {
        val lib = KlibAbiDumpMerger().apply {
            merge(loadAndRename(file("/merge/diverging/androidNativeArm64.api"), "android"))
            merge(loadAndRename(file("/merge/diverging/linuxArm64.api"), "linux"))
            merge(file("/merge/diverging/linuxX64.api"))
            merge(file("/merge/diverging/tvosX64.api"))
        }

        val dump = dumpToFile(lib)
        assertContentEquals(
            lines("/merge/diverging/merged_with_aliases_and_custom_names.abi"),
            Files.readAllLines(dump.toPath()).asSequence()
        )
    }

    @Test
    fun customTargetExtraction() {
        val lib = KlibAbiDumpMerger().apply {
            merge(file("/merge/diverging/merged_with_aliases_and_custom_names.abi"))
        }
        val targets = lib.targets.filter { it.targetName != "linuxArm64" }
        targets.forEach { lib.remove(it) }
        val extracted = dumpToFile(lib)
        assertContentEquals(
            lines("/merge/diverging/linuxArm64.extracted.api"),
            Files.readAllLines(extracted.toPath()).asSequence()
        )
    }

    @Test
    fun webTargetsExtraction() {
        val mergedPath = "/merge/webTargets/merged.abi"

        fun checkExtracted(targetName: String, expectedFile: String) {
            val lib = KlibAbiDumpMerger().apply { merge(file(mergedPath)) }
            val targets = lib.targets
            targets.filter { it.configurableName != targetName }.forEach { lib.remove(it) }
            val dump = dumpToFile(lib)
            assertContentEquals(
                lines(expectedFile),
                Files.readAllLines(dump.toPath()).asSequence(),
                "Dumps mismatched for target $targetName"
            )
        }

        checkExtracted("js", "/merge/webTargets/js.ext.abi")
        checkExtracted("wasmWasi", "/merge/webTargets/wasmWasi.ext.abi")
        checkExtracted("wasmJs", "/merge/webTargets/wasmJs.ext.abi")
    }

    @Test
    fun loadMultiTargetDump() {
        val lib = KlibAbiDumpMerger().apply {
            merge(file("/merge/stdlib_native_common.abi"))
        }
        val expectedTargetNames = listOf(
            "androidNativeArm32", "androidNativeArm64", "androidNativeX64", "androidNativeX86",
            "iosArm64", "iosSimulatorArm64", "iosX64", "linuxArm32Hfp", "linuxArm64", "linuxX64",
            "macosArm64", "macosX64", "mingwX64", "tvosArm64", "tvosSimulatorArm64", "tvosX64",
            "watchosArm32", "watchosArm64", "watchosDeviceArm64", "watchosSimulatorArm64", "watchosX64"
        )
        val expectedTargets = expectedTargetNames.asSequence().map(KlibTarget::parse).toSet()
        assertEquals(expectedTargets, lib.targets)

        assertFailsWith<IllegalArgumentException> {
            KlibAbiDumpMerger().merge(loadAndRename(file("/merge/stdlib_native_common.abi"), "target"))
        }
    }

    @Test
    fun mergeDumpsWithNonOverlappingDeclarations() {
        val dump = dumpToFile(KlibAbiDumpMerger().apply {
            merge(file("/merge/non-overlapping/linux-arm64.klib.abi"))
            merge(file("/merge/non-overlapping/linux-x64.klib.abi"))
        })

        assertContentEquals(
            lines("/merge/non-overlapping/merged.klib.abi"),
            Files.readAllLines(dump.toPath()).asSequence()
        )
    }

    @Test
    fun loadMergedDumpWithNonOverlappingDeclarations() {
        val dump = dumpToFile(KlibAbiDumpMerger().apply {
            merge(file("/merge/non-overlapping/merged.klib.abi"))
        })
        assertContentEquals(
            lines("/merge/non-overlapping/merged.klib.abi"),
            Files.readAllLines(dump.toPath()).asSequence()
        )
    }

    @Test
    fun parseDeclarationType() {
        val declarations = mapOf(
            "abstract class examples.classes/AC { // examples.classes/AC|null[0]" to DeclarationType.Class,
            "final class examples.classes/C { // examples.classes/C|null[0]" to DeclarationType.Class,
            "final inner class Inner { // examples.classes/Outer.Nested.Inner|null[0]" to DeclarationType.Class,
            "open annotation class examples.classes/A : kotlin/Annotation { // examples.classes/A|null[0]" to DeclarationType.AnnotationClass,
            "final object examples.classes/O // examples.classes/O|null[0]" to DeclarationType.Object,
            "abstract interface examples.classes/I // examples.classes/I|null[0]" to DeclarationType.Interface,
            "final value class classifiers.test/ValueClass { // classifiers.test/ValueClass|null[0]" to DeclarationType.Class,
            "abstract fun interface classifiers.test/FunctionInterface { // classifiers.test/FunctionInterface|null[0]" to DeclarationType.Interface,
            "final enum class examples.classes/E : kotlin/Enum<examples.classes/E> { // examples.classes/E|null[0]" to DeclarationType.EnumClass,

            "constructor <init>(kotlin/Int) // examples.classes/D.<init>|<init>(kotlin.Int){}[0]" to DeclarationType.Constructor,

            "final fun <get-entries>(): kotlin.enums/EnumEntries<examples.classes/E> // examples.classes/E.entries.<get-entries>|<get-entries>#static(){}[0]" to DeclarationType.Function,
            "final fun values(): kotlin/Array<examples.classes/E> // examples.classes/E.values|values#static(){}[0]" to DeclarationType.Function,
            "open fun o(): kotlin/Int // examples.classes/OC.o|o(){}[0]" to DeclarationType.Function,
            "final inline fun examples.classes/testInlineFun() // examples.classes/testInlineFun|testInlineFun(){}[0]" to DeclarationType.Function,
            "final fun <#A: kotlin/Any?> examples.classes/consume(#A) // examples.classes/consume|consume(0:0){0§<kotlin.Any?>}[0]" to DeclarationType.Function,
            "abstract fun a() // examples.classes/AC.a|a(){}[0]" to DeclarationType.Function,
            "final fun (kotlin/Int).callables.test/regularFun(): kotlin/String // callables.test/regularFun|regularFun@kotlin.Int(){}[0]" to DeclarationType.Function,
            "final fun context(kotlin/Number) (kotlin/Number).callables.test/regularFun(): kotlin/String // callables.test/regularFun|regularFun!kotlin.Int@kotlin.Number(){}[0]" to DeclarationType.Function,

            "final val entries // examples.classes/E.entries|#static{}entries[0]" to DeclarationType.Val,
            "final const val examples.classes/con // examples.classes/con|{}con[0]" to DeclarationType.ConstVal,
            "final var examples.classes/r // examples.classes/r|{}r[0]" to DeclarationType.Var,

            "enum entry A // examples.classes/E.A|null[0]" to DeclarationType.EnumEntry,

            "" to DeclarationType.Unknown,
            " " to DeclarationType.Unknown,
            "\t" to DeclarationType.Unknown
        )

        declarations.forEach { (line, expectedType) ->
            assertEquals(expectedType, DeclarationType.parseFromDeclaration(line), "Mismatch for line: '$line'")
        }
    }

    private fun loadAndRename(file: File, customTargetName: String): KlibAbiDumpMerger {
        val klib = KlibAbiDumpMerger()
        klib.merge(file)
        klib.overrideTargets(setOf(KlibTarget(klib.targets.single().targetName, customTargetName)))
        return klib
    }

    private fun loadAndRename(file: File, target: KlibTarget): KlibAbiDumpMerger {
        val klib = KlibAbiDumpMerger()
        klib.merge(file)
        klib.overrideTargets(setOf(target))
        return klib
    }
}
