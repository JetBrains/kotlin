/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlin.metadata.ClassKind
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.kind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

/**
 * Tests that Objective-C header extensions (`swift_name`, `objc_runtime_name`) are properly parsed
 * by `libclangext` and emitted into KLIB metadata when running `cinterop` in header mode (`-Xheader-mode`)
 * on a Linux host targeting `macos_x64`.
 */
class CInteropKlibCorrectnessTest : InteropTestsBase() {

    @BeforeEach
    fun onlyOnLinux() {
        Assumptions.assumeTrue(HostManager.hostIsLinux)
    }

    @Test
    fun testObjectiveCExtensionsInHeaderModeKlib() {
        val files = testFiles()
        val header = files.file("header.h", """
            __attribute__((swift_name("RenamedClass")))
            @interface Foo
            @end

            __attribute__((objc_runtime_name("CustomProtocol")))
            @protocol P
            @end
        """.trimIndent())

        val defFile = files.file("test.def", """
            language = Objective-C
            headers = ${header.name}
        """.trimIndent())

        val outputKlib = File(files.directory, "test.klib")
        generateHeaderModeKlib(defFile, header, outputKlib, files)

        val metadataDump = dumpKlibClasses(outputKlib)
        // Snapshot comparison guarantees all emitted declarations and annotations match without unintended symbols
        assertEquals(EXPECTED_METADATA_DUMP, metadataDump)
    }

    /**
     * Invokes `cinterop` in header-mode (`-Xheader-mode`) targeting `macos_x64`.
     *
     * In header-only mode on Linux, no Apple SDK or Xcode installation is present. We provide minimal
     * stand-alone declarations for standard C types (`stdint.h`, `string.h`) so Clang can parse the Objective-C
     * headers without needing macOS system headers.
     */
    private fun generateHeaderModeKlib(defFile: File, header: File, outputKlib: File, files: TempFiles) {
        val buildDir = File(files.directory, "test-build")
        val generatedDir = File(buildDir, "kotlin")
        val nativesDir = File(buildDir, "natives")
        val manifest = File(buildDir, "manifest.properties")
        val konanHome = findKonanHome()
        val propertyOverrides = System.getProperty("kotlin.native.propertyOverrides")

        // StubIrDriver injects stdint.h/string.h into all units. Standalone stubs satisfy Clang
        // on Linux without requiring a macOS SDK or conflicting host glibc headers.
        val sysrootInclude = File(buildDir, "sysroot/usr/include").apply {
            mkdirs()
            File(this, "stdint.h").writeText("""
                #ifndef _STDINT_H
                #define _STDINT_H
                typedef signed char int8_t;
                typedef short int16_t;
                typedef int int32_t;
                typedef long long int64_t;
                typedef unsigned char uint8_t;
                typedef unsigned short uint16_t;
                typedef unsigned int uint32_t;
                typedef unsigned long long uint64_t;
                typedef long intptr_t;
                typedef unsigned long uintptr_t;
                #endif
            """.trimIndent())
            File(this, "string.h").writeText("""
                #ifndef _STRING_H
                #define _STRING_H
                typedef unsigned long size_t;
                void *memcpy(void *dest, const void *src, size_t n);
                void *memset(void *s, int c, size_t n);
                size_t strlen(const char *s);
                #endif
            """.trimIndent())
        }

        val interop = org.jetbrains.kotlin.native.interop.gen.jvm.Interop()
        val result = interop.interop(
            flavor = "native",
            args = arrayOf(
                "-def", defFile.absolutePath,
                "-o", outputKlib.absolutePath,
                "-target", "macos_x64",
                "-Xheader-mode",
                "-no-default-libs",
                "-Xoverride-konan-properties", propertyOverrides,
                "-Xkonan-home", konanHome.absolutePath,
                "-compiler-option", "-I${header.parentFile.absolutePath}",
                "-compiler-option", "-isystem${sysrootInclude.absolutePath}"
            ),
            additionalArgs = org.jetbrains.kotlin.native.interop.gen.jvm.InternalInteropOptions(
                generated = generatedDir.absolutePath,
                natives = nativesDir.absolutePath,
                manifest = manifest.absolutePath,
                cstubsName = "cstubs"
            ),
            runFromDaemon = false
        )

        if (result != null) {
            error("cinterop header mode failed: ${result.joinToString("\n")}")
        }
    }

    /**
     * Reads the generated `.klib` and produces a formatted textual representation of all emitted classes,
     * companion objects, and their annotations for snapshot verification.
     */
    private fun dumpKlibClasses(outputKlib: File): String {
        val library = KlibLoader { libraryPaths(outputKlib.absolutePath) }.load().librariesStdlibFirst.single()
        val klibMetadata = library.metadata
        val module = KlibModuleMetadata.readStrict(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray = klibMetadata.moduleHeaderData
            override val metadataVersion: KlibMetadataVersion =
                KlibMetadataVersion(library.versions.metadataVersion!!.toArray())
            override fun packageMetadataParts(fqName: String): Set<String> =
                klibMetadata.getPackageFragmentNames(fqName)
            override fun packageMetadata(fqName: String, partName: String): ByteArray =
                klibMetadata.getPackageFragment(fqName, partName)
        })

        return module.fragments.flatMap { it.classes }.sortedBy { it.name }.joinToString("\n") { clazz ->
            val kind = when (clazz.kind) {
                ClassKind.CLASS -> "class"
                ClassKind.INTERFACE -> "interface"
                ClassKind.COMPANION_OBJECT -> "companion object"
                ClassKind.OBJECT -> "object"
                ClassKind.ENUM_CLASS -> "enum class"
                ClassKind.ENUM_ENTRY -> "enum entry"
                ClassKind.ANNOTATION_CLASS -> "annotation class"
            }
            val annotations = clazz.annotations
                .sortedBy { it.className }
                .map { annotation ->
                    val arguments = annotation.arguments.entries
                        .sortedBy { it.key }
                        .map { "${it.key}=${it.value.toSourceLiteral()}" }
                        .joinToString()
                    "${annotation.className.replace(".", "/")}($arguments)"
                }
                .joinToString()
            "$kind ${clazz.name} annotations: [$annotations]"
        }
    }

    private companion object {
        /**
         * Resolves the Kotlin/Native proto-distribution containing standard library KLIBs required for compilation.
         */
        fun findKonanHome(): File {
            val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
                .firstOrNull { File(it, "kotlin-native").exists() } ?: File(".")
            return File(root, "kotlin-native/libclangInterop/build").listFiles()?.firstOrNull {
                it.name.startsWith("nativeDistribution") && File(it, "klib/common/stdlib").exists()
            } ?: File(root, "kotlin-native/dist")
        }

        /**
         * Formats [KmAnnotationArgument] values into Kotlin-like literal representations for snapshot comparison.
         */
        fun KmAnnotationArgument.toSourceLiteral(): String = when (this) {
            is KmAnnotationArgument.StringValue -> "\"$value\""
            is KmAnnotationArgument.LiteralValue<*> -> value.toString()
            is KmAnnotationArgument.EnumValue -> "$enumClassName.$enumEntryName"
            is KmAnnotationArgument.AnnotationValue -> annotation.className + "(" + annotation.arguments.entries.joinToString { "${it.key}=${it.value.toSourceLiteral()}" } + ")"
            is KmAnnotationArgument.ArrayValue -> "[" + elements.joinToString { it.toSourceLiteral() } + "]"
            else -> toString()
        }

        val EXPECTED_METADATA_DUMP = """
            class test/Foo annotations: [kotlin/native/ObjCName(swiftName="RenamedClass"), kotlinx/cinterop/ExperimentalForeignApi(), kotlinx/cinterop/ExternalObjCClass()]
            companion object test/Foo.Companion annotations: []
            class test/FooMeta annotations: [kotlinx/cinterop/ExperimentalForeignApi(), kotlinx/cinterop/ExternalObjCClass()]
            interface test/PProtocol annotations: [kotlinx/cinterop/ExperimentalForeignApi(), kotlinx/cinterop/ExternalObjCClass(binaryName="CustomProtocol", protocolGetter="kniprot_test_P_0")]
            interface test/PProtocolMeta annotations: [kotlinx/cinterop/ExperimentalForeignApi(), kotlinx/cinterop/ExternalObjCClass(binaryName="CustomProtocol", protocolGetter="kniprot_test_P_0")]
        """.trimIndent()
    }
}
