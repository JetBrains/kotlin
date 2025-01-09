/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples

import org.jetbrains.kotlin.abi.tools.api.v2.KlibDump
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.abi.tools.v2.ToolsV2
import org.jetbrains.kotlin.abi.tools.v2.klib.inferAbi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.toString

class KlibDumpSamples {
    @JvmField
    @Rule
    var tempFolder = TemporaryFolder()

    fun createDumpFileWithContent(content: String): File {
        val file = tempFolder.newFile()
        file.writer().use {
            it.write(content)
        }
        return file
    }

    @Test
    fun mergeDumps() {
        val linuxX64Dump = createDumpFileWithContent("""
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
        """.trimIndent())

        val linuxArm64Dump = createDumpFileWithContent("""
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: false
            // - Show declarations: true

            // Library unique name: <org.example:bcv-klib-test>
            // Platform: NATIVE
            // Native targets: linux_arm64
            // Compiler version: 1.9.22
            // ABI version: 1.8.0
            final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
                final val value // org.example/ShardedClass.value|{}value[0]
                    final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]
                constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]
                final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
            }
        """.trimIndent())

        val mergedDump = ToolsV2.createKlibDump().apply {
            merge(linuxX64Dump)
            merge(linuxArm64Dump)
        }
        val mergedDumpContent = buildString { mergedDump.print(this) }

        assertEquals("""
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

                final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
            }

            // Targets: [linuxX64]
            final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]
            
            """.trimIndent(), mergedDumpContent)
    }

    @Test
    fun mergeDumpObjects() {
        val linuxX64Dump = createDumpFileWithContent("""
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
        """.trimIndent())

        val linuxArm64Dump = createDumpFileWithContent("""
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: false
            // - Show declarations: true

            // Library unique name: <org.example:bcv-klib-test>
            // Platform: NATIVE
            // Native targets: linux_arm64
            // Compiler version: 1.9.22
            // ABI version: 1.8.0
            final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
                final val value // org.example/ShardedClass.value|{}value[0]
                    final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]
                constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]
                final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
            }
        """.trimIndent())

        val mergedDump = ToolsV2.createKlibDump()
        mergedDump.merge(ToolsV2.loadKlibDump(linuxArm64Dump))
        mergedDump.merge(ToolsV2.loadKlibDump(linuxX64Dump).also { it.setCustomName("linuxX86_64") })
        val mergedDumpContent = buildString { mergedDump.print(this) }

        assertEquals("""
            // Klib ABI Dump
            // Targets: [linuxArm64, linuxX64.linuxX86_64]
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

            // Targets: [linuxX64.linuxX86_64]
            final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]
            
            """.trimIndent(), mergedDumpContent)

        mergedDump.remove(listOf(KlibTarget.parse("linuxX64.linuxX86_64")))
        val filteredDumpContent = buildString { mergedDump.print(this) }
        assertEquals("""
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

                final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
            }
            
            """.trimIndent(), filteredDumpContent)
    }

    @Test
    fun updateMergedDump() {
        val linuxX64Dump = """
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

        val linuxArm64Dump = """
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: false
            // - Show declarations: true

            // Library unique name: <org.example:bcv-klib-test>
            // Platform: NATIVE
            // Native targets: linux_arm64
            // Compiler version: 1.9.22
            // ABI version: 1.8.0
            final class org.example/ShardedClass { // org.example/ShardedClass|null[0]
                final val value // org.example/ShardedClass.value|{}value[0]
                    final fun <get-value>(): kotlin/Int // org.example/ShardedClass.value.<get-value>|<get-value>(){}[0]
                constructor <init>(kotlin/Int) // org.example/ShardedClass.<init>|<init>(kotlin.Int){}[0]
                final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
            }
        """.trimIndent()

        val mergedDump = ToolsV2.createKlibDump()
        mergedDump.merge(ToolsV2.loadKlibDump(linuxArm64Dump))
        mergedDump.merge(ToolsV2.loadKlibDump(linuxX64Dump).also { it.setCustomName("linuxX86_64") })

        val newLinuxX64Dump = """
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
                final fun store(kotlin/Int): kotlin/Int // org.example/ShardedClass.store|store(kotlin.Long){}[0]
            }
            final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]
        """.trimIndent()

        mergedDump.replace(ToolsV2.loadKlibDump(newLinuxX64Dump).also { it.setCustomName("linuxX86_64") })

        val mergedDumpContent = mergedDump.print(StringBuilder()).toString()
        assertEquals("""
            // Klib ABI Dump
            // Targets: [linuxArm64, linuxX64.linuxX86_64]
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
                final fun add(kotlin/Int): kotlin/Int // org.example/ShardedClass.add|add(kotlin.Int){}[0]
            
                // Targets: [linuxX64.linuxX86_64]
                final fun store(kotlin/Int): kotlin/Int // org.example/ShardedClass.store|store(kotlin.Long){}[0]
            }

            // Targets: [linuxX64.linuxX86_64]
            final fun org.example/ShardedClass(kotlin/Int, kotlin/Float, kotlin/Long): org.example/ShardedClass // org.example/ShardedClass|ShardedClass(kotlin.Int;kotlin.Float;kotlin.Long){}[0]
            
            """.trimIndent(), mergedDumpContent)
    }

    @Test
    fun extractTargets() {
        // Oh no, we're running on Windows and Apple targets are unsupported, let's filter it out!
        val mergedDump = createDumpFileWithContent("""
            // Klib ABI Dump
            // Targets: [iosArm64, iosSimulatorArm64, iosX64, linuxArm64, linuxX64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true

            // Library unique name: <testproject>
            abstract class examples.classes/Klass // examples.classes/Klass|null[0]            

            // Targets: [iosArm64, iosX64]
            abstract interface examples.classes/Iface // examples.classes/Iface|null[0]

        """.trimIndent())

        val dump = ToolsV2.loadKlibDump(mergedDump)
        assertEquals(
            listOf("iosArm64", "iosSimulatorArm64", "iosX64", "linuxArm64", "linuxX64").map(KlibTarget::parse).toSet(),
            dump.targets
        )
        // remove everything except linux*
        dump.retain(dump.targets.filter { it.targetName.startsWith("linux") })

        val filteredDumpContent = buildString { dump.print(this) }
        assertEquals("""
            // Klib ABI Dump
            // Targets: [linuxArm64, linuxX64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true

            // Library unique name: <testproject>
            abstract class examples.classes/Klass // examples.classes/Klass|null[0]            

        """.trimIndent(),
            filteredDumpContent)
    }

    @Test
    fun inferDump() {
        // We want to get a dump for iosArm64, but our host compiler doesn't support it.
        val unsupportedTarget = KlibTarget.parse("iosArm64")
        // Thankfully, we have an old merged dump ...
        val oldMergedDump = createDumpFileWithContent("""
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

        """.trimIndent())

        // ... and a new dump for linuxArm64
        val linuxDump = createDumpFileWithContent("""
            // Klib ABI Dump
            // Targets: [linuxArm64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true

            // Library unique name: <testproject>
            abstract class examples.classes/NewKlass // examples.classes/NewKlass|null[0]

        """.trimIndent())

        // Let's use these dumps to infer a public ABI on iosArm64
        val inferredIosArm64Dump = inferAbi(
            unsupportedTarget = unsupportedTarget,
            supportedTargetDumps = listOf(ToolsV2.loadKlibDump(linuxDump)),
            oldMergedDump = ToolsV2.loadKlibDump(oldMergedDump))

        assertEquals(unsupportedTarget, inferredIosArm64Dump.targets.single())

        val inferredDumpContent = buildString { inferredIosArm64Dump.print(this) }
        assertEquals("""
            // Klib ABI Dump
            // Targets: [iosArm64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true

            // Library unique name: <testproject>
            abstract interface examples.classes/Iface // examples.classes/Iface|null[0]

            abstract class examples.classes/NewKlass // examples.classes/NewKlass|null[0]

        """.trimIndent(),
            inferredDumpContent)
    }

    private fun KlibDump.setCustomName(customName: String) {
        renameSingleTarget(KlibTarget(targets.first().targetName, customName))
    }
}
