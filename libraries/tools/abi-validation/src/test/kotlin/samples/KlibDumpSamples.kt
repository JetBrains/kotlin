/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package samples

import kotlinx.validation.ExperimentalBCVApi
import kotlinx.validation.api.klib.KlibDump
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.inferAbi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class KlibDumpSamples {
    @JvmField
    @Rule
    var tempFolder = TemporaryFolder()

    fun createDumpWithContent(content: String): File {
        val file = tempFolder.newFile()
        file.writer().use {
            it.write(content)
        }
        return file
    }

    @OptIn(ExperimentalBCVApi::class)
    @Test
    fun mergeDumps() {
        val linuxX64Dump = createDumpWithContent("""
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

        val linuxArm64Dump = createDumpWithContent("""
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

        val mergedDump = KlibDump().apply {
            merge(linuxX64Dump)
            merge(linuxArm64Dump)
        }
        val mergedDumpContent = buildString { mergedDump.saveTo(this) }

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

    @OptIn(ExperimentalBCVApi::class)
    @Test
    fun mergeDumpObjects() {
        val linuxX64Dump = createDumpWithContent("""
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

        val linuxArm64Dump = createDumpWithContent("""
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

        val mergedDump = KlibDump()
        mergedDump.merge(KlibDump.from(linuxArm64Dump))
        mergedDump.merge(KlibDump.from(linuxX64Dump, configurableTargetName = "linuxX86_64"))
        val mergedDumpContent = buildString { mergedDump.saveTo(this) }

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
        val filteredDumpContent = buildString { mergedDump.saveTo(this) }
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

    @OptIn(ExperimentalBCVApi::class)
    @Test
    fun extractTargets() {
        // Oh no, we're running on Windows and Apple targets are unsupported, let's filter it out!
        val mergedDump = createDumpWithContent("""
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

        val dump = KlibDump.from(mergedDump)
        assertEquals(
            listOf("iosArm64", "iosSimulatorArm64", "iosX64", "linuxArm64", "linuxX64").map(KlibTarget::parse).toSet(),
            dump.targets
        )
        // remove everything except linux*
        dump.retain(dump.targets.filter { it.targetName.startsWith("linux") })

        val filteredDumpContent = buildString { dump.saveTo(this) }
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

    @OptIn(ExperimentalBCVApi::class)
    @Test
    fun inferDump() {
        // We want to get a dump for iosArm64, but our host compiler doesn't support it.
        val unsupportedTarget = KlibTarget.parse("iosArm64")
        // Thankfully, we have an old merged dump ...
        val oldMergedDump = createDumpWithContent("""
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
        val linuxDump = createDumpWithContent("""
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
            supportedTargetDumps = listOf(KlibDump.from(linuxDump)),
            oldMergedDump = KlibDump.from(oldMergedDump))

        assertEquals(unsupportedTarget, inferredIosArm64Dump.targets.single())

        val inferredDumpContent = buildString { inferredIosArm64Dump.saveTo(this) }
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
}
