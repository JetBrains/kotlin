/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.KlibDump
import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileNotFoundException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private val rawLinuxDump = """
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: false
    // - Show declarations: true

    // Library unique name: <org.example:bcv-klib-test>
    // Platform: NATIVE
    // Native targets: linux_x64
    // Compiler version: 1.9.22
    // ABI version: 1.8.0
    final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
        final val value // org.example/ShardedClass.value|{}value[0]
            final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]
        constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]
        final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
    }
    final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]
""".trimIndent()

private val mergedLinuxDump = """
    // Klib ABI Dump
    // Targets: [linuxX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: false
    // - Show declarations: true

    // Library unique name: <org.example:bcv-klib-test>
    final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
        constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]

        final val value // org.example/ShardedClass.value|{}value[0]
            final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]

        final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
    }

    final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]

""".trimIndent()


private val mergedLinuxDumpWithTargetSpecificDeclaration = """
    // Klib ABI Dump
    // Targets: [linuxArm64, linuxX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: false
    // - Show declarations: true

    // Library unique name: <org.example:bcv-klib-test>
    final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
        constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]

        final val value // org.example/ShardedClass.value|{}value[0]
            final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]

        // Targets: [linuxArm64]
        final fun add2(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]

        // Targets: [linuxX64]
        final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
    }

    // Targets: [linuxArm64]
    final fun org.example/ShardedClass2(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]

    // Targets: [linuxX64]
    final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]

""".trimIndent()

private val mergedLinuxArm64Dump = """
    // Klib ABI Dump
    // Targets: [linuxArm64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: false
    // - Show declarations: true

    // Library unique name: <org.example:bcv-klib-test>
    final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
        constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]

        final val value // org.example/ShardedClass.value|{}value[0]
            final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]

        final fun add2(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
    }

    final fun org.example/ShardedClass2(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]

""".trimIndent()

private val mergedLinuxDumpWithCustomName = """
    // Klib ABI Dump
    // Targets: [linuxX64.testTarget]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: false
    // - Show declarations: true

    // Library unique name: <org.example:bcv-klib-test>
    final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
        constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]

        final val value // org.example/ShardedClass.value|{}value[0]
            final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]

        final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
    }

    final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]

""".trimIndent()

private val rawMultitargetDump = """
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true

    // Library unique name: <stdlib>
    // Platform: NATIVE
    // Native targets: android_arm32, android_arm64, android_x64, android_x86, ios_arm64, ios_simulator_arm64, ios_x64, linux_arm32_hfp, linux_arm64, linux_x64, macos_arm64, macos_x64, mingw_x64, tvos_arm64, tvos_simulator_arm64, tvos_x64, watchos_arm32, watchos_arm64, watchos_device_arm64, watchos_simulator_arm64, watchos_x64
    // Compiler version: 2.0.255-SNAPSHOT
    // ABI version: 1.8.0
    abstract interface kotlin/Annotation // kotlin/Annotation|null[0]
""".trimIndent()

private val mergedMultitargetDump = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, iosArm64, iosSimulatorArm64, iosX64, linuxArm32Hfp, linuxArm64, linuxX64, macosArm64, macosX64, mingwX64, tvosArm64, tvosSimulatorArm64, tvosX64, watchosArm32, watchosArm64, watchosDeviceArm64, watchosSimulatorArm64, watchosX64]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true
    
    // Library unique name: <stdlib>
    abstract interface kotlin/Annotation // kotlin/Annotation|null[0]
    
""".trimIndent()


private val mergedMultitargetDumpFiltered = """
    // Klib ABI Dump
    // Targets: [androidNativeArm32]
    // Rendering settings:
    // - Signature version: 2
    // - Show manifest properties: true
    // - Show declarations: true
    
    // Library unique name: <stdlib>
    abstract interface kotlin/Annotation // kotlin/Annotation|null[0]
    
""".trimIndent()

// Note that some cases are already covered in KlibDumpSamples.kt and not duplicated here
class KlibDumpTest {
    @JvmField
    @Rule
    var tmpFolder = TemporaryFolder()

    @Test
    fun emptyDump() {
        val dump = buildString {
            AbiToolsImpl.createKlibDump().print(this)
        }
        assertEquals("", dump)

        assertFailsWith<IllegalArgumentException> {
            AbiToolsImpl.loadKlibDump(tmpFolder.newFile())
        }
    }

    @Test
    fun loadFromNonExistingFile() {
        assertFailsWith<FileNotFoundException> {
            AbiToolsImpl.loadKlibDump(tmpFolder.root.resolve(UUID.randomUUID().toString()))
        }
        assertFailsWith<FileNotFoundException> {
            AbiToolsImpl.createKlibDump().merge(tmpFolder.root.resolve(UUID.randomUUID().toString()))
        }
        assertFailsWith<FileNotFoundException> {
            AbiToolsImpl.loadKlibDump(tmpFolder.root.resolve(UUID.randomUUID().toString()))
        }
        assertFailsWith<FileNotFoundException> {
            AbiToolsImpl.createKlibDump().mergeFromKlib(tmpFolder.root.resolve(UUID.randomUUID().toString()))
        }
    }

    @Test
    fun loadKlibFromNonKlib() {
        assertFailsWith<IllegalArgumentException> { AbiToolsImpl.loadKlibDump(tmpFolder.root) }
        assertFailsWith<IllegalArgumentException> { AbiToolsImpl.loadKlibDump(tmpFolder.newFile()) }

        assertFailsWith<IllegalStateException> { AbiToolsImpl.createKlibDump().also { it.mergeFromKlib(tmpFolder.root) } }
        assertFailsWith<IllegalStateException> { AbiToolsImpl.createKlibDump().also { it.mergeFromKlib(tmpFolder.newFile()) } }
    }

    @Test
    fun loadFromDirectory() {
        assertFailsWith<IllegalArgumentException> {
            AbiToolsImpl.loadKlibDump(tmpFolder.root)
        }
        assertFailsWith<IllegalArgumentException> {
            AbiToolsImpl.createKlibDump().merge(tmpFolder.root)
        }
    }

    @Test
    fun loadDumpWithSingleTarget() {
        val klibDump = AbiToolsImpl.loadKlibDump(rawLinuxDump)
        assertEquals(setOf(KlibTarget.parse("linuxX64")), klibDump.targets)
        assertEquals(mergedLinuxDump, buildString { klibDump.print(this) })

        val mergedKlibDump = AbiToolsImpl.loadKlibDump(mergedLinuxDump)
        assertEquals(setOf(KlibTarget.parse("linuxX64")), mergedKlibDump.targets)
        assertEquals(mergedLinuxDump, buildString { mergedKlibDump.print(this) })
    }

    @Test
    fun mergeDumpWithSingleTarget() {
        val klibDump = AbiToolsImpl.createKlibDump().also { it.merge(rawLinuxDump) }
        assertEquals(setOf(KlibTarget.parse("linuxX64")), klibDump.targets)
        assertEquals(mergedLinuxDump, buildString { klibDump.print(this) })

        val mergedKlibDump = AbiToolsImpl.createKlibDump().also { it.merge(mergedLinuxDump) }
        assertEquals(setOf(KlibTarget.parse("linuxX64")), mergedKlibDump.targets)
        assertEquals(mergedLinuxDump, buildString { mergedKlibDump.print(this) })
    }

    @Test
    fun loadDumpWithSingleTargetWithCustomName() {
        val klibDump = AbiToolsImpl.loadKlibDump(rawLinuxDump)
        klibDump.setCustomName("testTarget")
        assertEquals(setOf(KlibTarget.parse("linuxX64.testTarget")), klibDump.targets)
        assertEquals(mergedLinuxDumpWithCustomName, buildString { klibDump.print(this) })

        val mergedKlibDump = AbiToolsImpl.loadKlibDump(mergedLinuxDump)
        mergedKlibDump.setCustomName("testTarget")
        assertEquals(setOf(KlibTarget.parse("linuxX64.testTarget")), mergedKlibDump.targets)
        assertEquals(mergedLinuxDumpWithCustomName, buildString { mergedKlibDump.print(this) })

        val customTargetDump = AbiToolsImpl.loadKlibDump(mergedLinuxDumpWithCustomName)
        assertEquals(setOf(KlibTarget.parse("linuxX64.testTarget")), customTargetDump.targets)
        assertEquals(mergedLinuxDumpWithCustomName, buildString { customTargetDump.print(this) })
    }

    @Test
    fun mergeDumpWithSingleTargetWithCustomName() {
        val klibDump = AbiToolsImpl.createKlibDump().also { it.merge(rawLinuxDump) }
        klibDump.setCustomName("testTarget")
        assertEquals(setOf(KlibTarget.parse("linuxX64.testTarget")), klibDump.targets)
        assertEquals(mergedLinuxDumpWithCustomName, buildString { klibDump.print(this) })

        val mergedKlibDump =
            AbiToolsImpl.createKlibDump().also { it.merge(mergedLinuxDump) }
        mergedKlibDump.setCustomName("testTarget")
        assertEquals(setOf(KlibTarget.parse("linuxX64.testTarget")), mergedKlibDump.targets)
        assertEquals(mergedLinuxDumpWithCustomName, buildString { mergedKlibDump.print(this) })

        val customTargetDump = AbiToolsImpl.createKlibDump().also { it.merge(mergedLinuxDumpWithCustomName) }
        assertEquals(setOf(KlibTarget.parse("linuxX64.testTarget")), customTargetDump.targets)
        assertEquals(mergedLinuxDumpWithCustomName, buildString { customTargetDump.print(this) })
    }

    @Test
    fun loadMultitargetDump() {
        val dump = AbiToolsImpl.loadKlibDump(rawMultitargetDump)
        assertEquals(21, dump.targets.size)
        assertEquals(mergedMultitargetDump, buildString { dump.print(this) })

        val mergedDump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        assertEquals(21, mergedDump.targets.size)
        assertEquals(mergedMultitargetDump, buildString { mergedDump.print(this) })
    }

    @Test
    fun mergeMultitargetDump() {
        val dump = AbiToolsImpl.createKlibDump().also { it.merge(rawMultitargetDump) }
        assertEquals(21, dump.targets.size)
        assertEquals(mergedMultitargetDump, buildString { dump.print(this) })

        val mergedDump = AbiToolsImpl.createKlibDump().also { it.merge(mergedMultitargetDump) }
        assertEquals(21, mergedDump.targets.size)
        assertEquals(mergedMultitargetDump, buildString { mergedDump.print(this) })
    }

    @Test
    fun loadMultitargetDumpUsingCustomName() {
        assertFailsWith<IllegalStateException> {
            val dump = AbiToolsImpl.loadKlibDump(rawMultitargetDump)
            dump.setCustomName("abc")
        }
    }

    @Test
    fun retainAll() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        val oldTargets = setOf(*dump.targets.toTypedArray())

        dump.retain(oldTargets)
        assertEquals(oldTargets, dump.targets)
        assertEquals(mergedMultitargetDump, buildString { dump.print(this) })
    }

    @Test
    fun retainSingle() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        val singleTarget = KlibTarget.parse("androidNativeArm32")

        dump.retain(setOf(singleTarget))
        assertEquals(setOf(singleTarget), dump.targets)
        assertEquals(mergedMultitargetDumpFiltered, buildString { dump.print(this) })
    }

    @Test
    fun retainNone() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        dump.retain(emptySet())

        assertTrue(dump.targets.isEmpty())
        assertEquals("", buildString { dump.print(this) })
    }

    @Test
    fun removeAll() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        dump.remove(listOf(*dump.targets.toTypedArray()))

        assertTrue(dump.targets.isEmpty())
        assertEquals("", buildString { dump.print(this) })
    }

    @Test
    fun removeNone() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        val oldTargets = setOf(*dump.targets.toTypedArray())

        dump.remove(emptySet())
        assertEquals(oldTargets, dump.targets)
        assertEquals(mergedMultitargetDump, buildString { dump.print(this) })
    }

    @Test
    fun removeSome() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        val singleTarget = KlibTarget.parse("androidNativeArm32")

        dump.remove(dump.targets.subtract(setOf(singleTarget)))
        assertEquals(setOf(singleTarget), dump.targets)
        assertEquals(mergedMultitargetDumpFiltered, buildString { dump.print(this) })
    }

    @Test
    fun removeOrRetainTargetsNotPresentedInDump() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        val targets = setOf(*dump.targets.toTypedArray())
        dump.remove(listOf(KlibTarget.parse("linuxX64.blablabla")))
        assertEquals(targets, dump.targets)

        dump.retain(listOf(KlibTarget.parse("macosArm64.macos")))
        assertTrue(dump.targets.isEmpty())
    }

    @Test
    fun removeDeclarationsAlongWithTargets() {
        val dump = AbiToolsImpl.loadKlibDump(mergedLinuxDumpWithTargetSpecificDeclaration)
        val toRemove = KlibTarget.parse("linuxArm64")

        dump.remove(listOf(toRemove))
        assertEquals(mergedLinuxDump, buildString { dump.print(this) })
    }

    @Test
    fun testCopy() {
        val dump = AbiToolsImpl.loadKlibDump(mergedLinuxDumpWithTargetSpecificDeclaration)
        val copy = dump.copy()

        dump.remove(listOf(KlibTarget.parse("linuxArm64")))
        assertEquals(mergedLinuxDumpWithTargetSpecificDeclaration, buildString { copy.print(this) })
    }

    @Test
    fun testMergeDumps() {
        val dump = AbiToolsImpl.createKlibDump().also {
            it.merge(mergedLinuxDump)
            it.merge(mergedLinuxArm64Dump)
        }
        assertEquals(mergedLinuxDumpWithTargetSpecificDeclaration, buildString { dump.print(this) })
    }

    @Test
    fun mergeDumpsWithIntersectingTargets() {
        val mergedDump = AbiToolsImpl.loadKlibDump(rawMultitargetDump)

        assertFailsWith<IllegalStateException> {
            mergedDump.merge(rawMultitargetDump)
        }

        assertFailsWith<IllegalStateException> {
            mergedDump.merge(mergedLinuxDump)
        }
    }

    @Test
    fun inferWithoutAnOldDump() {
        val unsupportedTarget = KlibTarget.parse("iosArm64")

        val linuxDump = AbiToolsImpl.loadKlibDump(
            """
                // Klib ABI Dump
                // Targets: [linuxArm64]
                // Rendering settings:
                // - Signature version: 2
                // - Show manifest properties: true
                // - Show declarations: true
    
                // Library unique name: <testproject>
                abstract class examples.classes/NewKlass // examples.classes/NewKlass|null[0]
            """.trimIndent()
        )

        // Let's use these dumps to infer a public ABI on iosArm64
        val inferredIosArm64Dump = linuxDump.inferAbiForUnsupportedTarget(AbiToolsImpl.createKlibDump(), unsupportedTarget)

        assertEquals(unsupportedTarget, inferredIosArm64Dump.targets.single())

        val inferredDumpContent = buildString { inferredIosArm64Dump.print(this) }
        assertEquals(
            """
                // Klib ABI Dump
                // Targets: [iosArm64]
                // Rendering settings:
                // - Signature version: 2
                // - Show manifest properties: true
                // - Show declarations: true
    
                // Library unique name: <testproject>
                abstract class examples.classes/NewKlass // examples.classes/NewKlass|null[0]

            """.trimIndent(), inferredDumpContent
        )
    }

    @Test
    fun inferFromAnOldDumpOnly() {
        val unsupportedTarget = KlibTarget.parse("iosArm64")

        val oldDump = AbiToolsImpl.loadKlibDump(
            """
            // Klib ABI Dump
            // Targets: [iosArm64, linuxArm64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true

            // Library unique name: <testproject>
            abstract class examples.classes/Klass // examples.classes/Klass|null[0]
            // Targets: [iosArm64]
            abstract interface examples.classes/Iface // examples.classes/Iface|null[0]

        """.trimIndent()
        )

        // Let's use these dumps to infer a public ABI on iosArm64
        val inferredIosArm64Dump = AbiToolsImpl.createKlibDump().inferAbiForUnsupportedTarget(oldDump, unsupportedTarget)

        assertEquals(unsupportedTarget, inferredIosArm64Dump.targets.single())

        val inferredDumpContent = buildString { inferredIosArm64Dump.print(this) }
        assertEquals(
            """
                // Klib ABI Dump
                // Targets: [iosArm64]
                // Rendering settings:
                // - Signature version: 2
                // - Show manifest properties: true
                // - Show declarations: true
    
                // Library unique name: <testproject>
                abstract interface examples.classes/Iface // examples.classes/Iface|null[0]
                
                abstract class examples.classes/Klass // examples.classes/Klass|null[0]

            """.trimIndent(), inferredDumpContent
        )
    }

    @Test
    fun inferOutOfThinAir() {
        val unsupportedTarget = KlibTarget.parse("iosArm64")

        assertFailsWith<IllegalStateException> {
            AbiToolsImpl.createKlibDump().inferAbiForUnsupportedTarget(AbiToolsImpl.createKlibDump(), unsupportedTarget)
        }
    }

    @Test
    fun inferFromSelf() {
        val dump = AbiToolsImpl.loadKlibDump(mergedLinuxDump)
        assertFailsWith<IllegalArgumentException> {
            dump.inferAbiForUnsupportedTarget(AbiToolsImpl.createKlibDump(), dump.targets.first())
        }
    }

    @Test
    fun inferFromIntersectingDumps() {
        assertFailsWith<IllegalStateException> {
            AbiToolsImpl.loadKlibDump(mergedLinuxDump).merge(mergedMultitargetDump)
        }
    }

    @Test
    fun iterativeGrouping() {
        val dump = AbiToolsImpl.loadKlibDump(
            """
            // Klib ABI Dump
            // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64, mingwX64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <testproject>
            final class org.different.pack/BuildConfig { // org.different.pack/BuildConfig|null[0]
                constructor <init>() // org.different.pack/BuildConfig.<init>|<init>(){}[0]
                final val p1 // org.different.pack/BuildConfig.p1|{}p1[0]
                    final fun <get-p1>(): kotlin/Int // org.different.pack/BuildConfig.p1.<get-p1>|<get-p1>(){}[0]
                final fun f1(): kotlin/Int // org.different.pack/BuildConfig.f1|f1(){}[0]
            }
            // Targets: [androidNativeArm64]
            final fun (org.different.pack/BuildConfig).org.different.pack/linuxArm64Specific(): kotlin/Int // org.different.pack/linuxArm64Specific|linuxArm64Specific@org.different.pack.BuildConfig(){}[0]
            // Targets: [linuxArm64, linuxX64]
            final fun (org.different.pack/BuildConfig).org.different.pack/linuxArm64Specific2(): kotlin/Int // org.different.pack/linuxArm64Specific2|linuxArm64Specific@org.different.pack.BuildConfig(){}[0]
            // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64]
            final fun (org.different.pack/BuildConfig).org.different.pack/linuxArm64Specific3(): kotlin/Int // org.different.pack/linuxArm64Specific3|linuxArm64Specific@org.different.pack.BuildConfig(){}[0]
            
        """.trimIndent()
        )

        val expectedDump = """
            // Klib ABI Dump
            // Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86, linuxArm64, linuxX64, mingwX64]
            // Alias: androidNative => [androidNativeArm32, androidNativeArm64, androidNativeX64, androidNativeX86]
            // Alias: linux => [linuxArm64, linuxX64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <testproject>
            final class org.different.pack/BuildConfig { // org.different.pack/BuildConfig|null[0]
                constructor <init>() // org.different.pack/BuildConfig.<init>|<init>(){}[0]

                final val p1 // org.different.pack/BuildConfig.p1|{}p1[0]
                    final fun <get-p1>(): kotlin/Int // org.different.pack/BuildConfig.p1.<get-p1>|<get-p1>(){}[0]

                final fun f1(): kotlin/Int // org.different.pack/BuildConfig.f1|f1(){}[0]
            }

            // Targets: [androidNative, linux]
            final fun (org.different.pack/BuildConfig).org.different.pack/linuxArm64Specific3(): kotlin/Int // org.different.pack/linuxArm64Specific3|linuxArm64Specific@org.different.pack.BuildConfig(){}[0]

            // Targets: [linux]
            final fun (org.different.pack/BuildConfig).org.different.pack/linuxArm64Specific2(): kotlin/Int // org.different.pack/linuxArm64Specific2|linuxArm64Specific@org.different.pack.BuildConfig(){}[0]

            // Targets: [androidNativeArm64]
            final fun (org.different.pack/BuildConfig).org.different.pack/linuxArm64Specific(): kotlin/Int // org.different.pack/linuxArm64Specific|linuxArm64Specific@org.different.pack.BuildConfig(){}[0]

        """.trimIndent()
        assertEquals(expectedDump, buildString { dump.print(this) })
    }

    @Test
    fun similarGroupRemoval() {
        // native function should use a group alias "ios", not "apple", or "native"
        val dump = AbiToolsImpl.loadKlibDump(
            """
            // Klib ABI Dump
            // Targets: [iosArm64, iosX64, js]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <testproject>
            final fun org.example/common(): kotlin/Int // com.example/common|common(){}[0]
            // Targets: [iosArm64, iosX64]
            final fun org.example/native(): kotlin/Int // com.example/native|native(){}[0]
            
        """.trimIndent()
        )

        val expectedDump = """
            // Klib ABI Dump
            // Targets: [iosArm64, iosX64, js]
            // Alias: ios => [iosArm64, iosX64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <testproject>
            final fun org.example/common(): kotlin/Int // com.example/common|common(){}[0]

            // Targets: [ios]
            final fun org.example/native(): kotlin/Int // com.example/native|native(){}[0]
            
        """.trimIndent()
        assertEquals(expectedDump, buildString { dump.print(this) })
    }

    @Test
    fun saveToFile() {
        val dump = AbiToolsImpl.loadKlibDump(mergedMultitargetDump)
        val tempFile = tmpFolder.newFile()
        dump.print(tempFile)

        assertEquals(
            buildString { dump.print(this) },
            tempFile.readText(Charsets.US_ASCII)
        )
    }

    @Test
    fun declarationsOrdering() {
        val dump = AbiToolsImpl.loadKlibDump(
            """
            // Klib ABI Dump
            // Targets: [iosArm64, iosX64, js]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <testproject>
            final const val c/con // c/con|{}con[0]
                final fun <get-con>(): kotlin/String // c/con.<get-con>|<get-con>(){}[0]
            final class cls/D // cls/D|null[0]
            final fun f/b: kotlin/Int // f/b|b(){}[0]
            abstract interface an.iface/I // an.iface/I|null[0]
            final const val c/acon // c/acon|{}acon[0]
                final fun <get-acon>(): kotlin/String // c/acon.<get-acon>|<get-acon>(){}[0]
            abstract class cls/C // cls/C|null[0]
            final fun f/a: kotlin/Int // f/a|a(){}[0]
            open annotation class ann/B : kotlin/Annotation { // ann/B|null[0]
                constructor <init>() // ann/B.<init>|<init>(){}[0]
            }
            final var v/e // v/e|{}d[0]
                final fun <get-e>(): kotlin/Double // v/d.<get-e>|<get-e>(){}[0]
                final fun <set-e>(kotlin/Double) // v/e.<set-e>|<set-e>(kotlin.Double){}[0]
            final class cls/B { // cls/B|null[0]
                final fun b(): kotlin/Int // cls/B.b|b(){}[0]
                final var aaa // cls/B.aaa|{}aaa[0]
                    final fun <set-aaa>(kotlin/Int) // cls/B.aaa.<set-aaa>|<set-aaa>(kotlin.Int){}[0]
                    final fun <get-aaa>(): kotlin/Int // cls/B.aaa.<get-aaa>|<get-aaa>(){}[0]
                final val y // cls/B.y|{}y[0]
                    final fun <get-y>(): kotlin/Int // cls/B.y.<get-y>|<get-y>(){}[0]
                final var yy // cls/B.yy|{}yy[0]
                    final fun <get-yy>(): kotlin/Int // cls/B.yy.<get-yy>|<get-yy>(){}[0]
                    final fun <set-yy>(kotlin/Int) // cls/B.yy.<set-yy>|<set-yy>(kotlin.Int){}[0]
                final inner class I2 // cls/B.I2|null[0]
                final inner class I1 // cls/B.I1|null[0]
                final fun a(): kotlin/Int // cls/B.a|a(){}[0]
                final class N // cls/B.N|null[0]
                final class A // cls/B.A|null[0]
                final val x // cls/B.x|{}x[0]
                    final fun <get-x>(): kotlin/Int // cls/B.x.<get-x>|<get-x>(){}[0]
                final fun c(): kotlin/Int // cls/B.c|c(){}[0]
                constructor <init>(kotlin/Int) // cls/B.<init>|<init>(kotlin.Int){}[0]
                constructor <init>() // cls/B.<init>|<init>(){}[0]
            }
            final val v/l // v/l|{}l[0]
                final fun <get-l>(): kotlin/Long // v/l.<get-l>|<get-l>(){}[0]
            final val v/a // v/a|{}a[0]
                final fun <get-a>(): kotlin/Long // v/a.<get-a>|<get-a>(){}[0]
            final var v/d // v/d|{}d[0]
                final fun <set-d>(kotlin/Double) // v/d.<set-d>|<set-d>(kotlin.Double){}[0]
                final fun <get-d>(): kotlin/Double // v/d.<get-d>|<get-d>(){}[0]
            abstract interface iface/I // iface/I|null[0]
            open annotation class ann/A : kotlin/Annotation { // ann/A|null[0]
                constructor <init>() // ann/A.<init>|<init>(){}[0]
            }
            final enum class a/E : kotlin/Enum<a/E> { // a/eE|null[0]
                enum entry C // a/E.C|null[0]
                enum entry B // a/E.B|null[0]
                enum entry A // a/E.A|null[0]            
            }
            final object a/O // a/O|null[0]
            final object a/OO // a/OO|null[0]
            final class cls/A // cls/A|null[0]

        """.trimIndent()
        )

        val expectedDump = """
            // Klib ABI Dump
            // Targets: [iosArm64, iosX64, js]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <testproject>
            open annotation class ann/A : kotlin/Annotation { // ann/A|null[0]
                constructor <init>() // ann/A.<init>|<init>(){}[0]
            }
            
            open annotation class ann/B : kotlin/Annotation { // ann/B|null[0]
                constructor <init>() // ann/B.<init>|<init>(){}[0]
            }
            
            final enum class a/E : kotlin/Enum<a/E> { // a/eE|null[0]
                enum entry A // a/E.A|null[0]            
                enum entry B // a/E.B|null[0]
                enum entry C // a/E.C|null[0]
            }
            
            abstract interface an.iface/I // an.iface/I|null[0]
            
            abstract interface iface/I // iface/I|null[0]
            
            abstract class cls/C // cls/C|null[0]
            
            final class cls/A // cls/A|null[0]
            
            final class cls/B { // cls/B|null[0]
                constructor <init>() // cls/B.<init>|<init>(){}[0]
                constructor <init>(kotlin/Int) // cls/B.<init>|<init>(kotlin.Int){}[0]
            
                final val x // cls/B.x|{}x[0]
                    final fun <get-x>(): kotlin/Int // cls/B.x.<get-x>|<get-x>(){}[0]
                final val y // cls/B.y|{}y[0]
                    final fun <get-y>(): kotlin/Int // cls/B.y.<get-y>|<get-y>(){}[0]
            
                final var aaa // cls/B.aaa|{}aaa[0]
                    final fun <get-aaa>(): kotlin/Int // cls/B.aaa.<get-aaa>|<get-aaa>(){}[0]
                    final fun <set-aaa>(kotlin/Int) // cls/B.aaa.<set-aaa>|<set-aaa>(kotlin.Int){}[0]
                final var yy // cls/B.yy|{}yy[0]
                    final fun <get-yy>(): kotlin/Int // cls/B.yy.<get-yy>|<get-yy>(){}[0]
                    final fun <set-yy>(kotlin/Int) // cls/B.yy.<set-yy>|<set-yy>(kotlin.Int){}[0]
            
                final fun a(): kotlin/Int // cls/B.a|a(){}[0]
                final fun b(): kotlin/Int // cls/B.b|b(){}[0]
                final fun c(): kotlin/Int // cls/B.c|c(){}[0]
            
                final class A // cls/B.A|null[0]
            
                final class N // cls/B.N|null[0]
            
                final inner class I1 // cls/B.I1|null[0]
            
                final inner class I2 // cls/B.I2|null[0]
            }
            
            final class cls/D // cls/D|null[0]
            
            final object a/O // a/O|null[0]
            
            final object a/OO // a/OO|null[0]
            
            final const val c/acon // c/acon|{}acon[0]
                final fun <get-acon>(): kotlin/String // c/acon.<get-acon>|<get-acon>(){}[0]
            final const val c/con // c/con|{}con[0]
                final fun <get-con>(): kotlin/String // c/con.<get-con>|<get-con>(){}[0]
            
            final val v/a // v/a|{}a[0]
                final fun <get-a>(): kotlin/Long // v/a.<get-a>|<get-a>(){}[0]
            final val v/l // v/l|{}l[0]
                final fun <get-l>(): kotlin/Long // v/l.<get-l>|<get-l>(){}[0]
            
            final var v/d // v/d|{}d[0]
                final fun <get-d>(): kotlin/Double // v/d.<get-d>|<get-d>(){}[0]
                final fun <set-d>(kotlin/Double) // v/d.<set-d>|<set-d>(kotlin.Double){}[0]
            final var v/e // v/e|{}d[0]
                final fun <get-e>(): kotlin/Double // v/d.<get-e>|<get-e>(){}[0]
                final fun <set-e>(kotlin/Double) // v/e.<set-e>|<set-e>(kotlin.Double){}[0]
            
            final fun f/a: kotlin/Int // f/a|a(){}[0]
            final fun f/b: kotlin/Int // f/b|b(){}[0]
            
        """.trimIndent()
        assertEquals(expectedDump, buildString { dump.print(this) })
    }

    private fun KlibDump.setCustomName(customName: String) {
        renameSingleTarget(KlibTarget(targets.first().targetName, customName))
    }
}
