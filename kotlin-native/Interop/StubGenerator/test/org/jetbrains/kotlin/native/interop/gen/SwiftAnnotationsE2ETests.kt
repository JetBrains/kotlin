/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.klib.KlibMetadataVersion
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.gen.jvm.CCallMode
import org.jetbrains.kotlin.native.interop.gen.jvm.GenerationMode
import org.jetbrains.kotlin.native.interop.gen.jvm.InteropConfiguration
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*
import org.jetbrains.kotlin.util.toCInteropKlibMetadataVersion
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end tests for Swift annotation propagation (SwiftName and SwiftBridge)
 * from Objective-C headers through the cinterop pipeline to generated Kotlin stubs.
 */
class SwiftAnnotationsE2ETests : InteropTestsBase() {

    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    // ==================== SwiftName Tests ====================

    @Test
    fun `class with swift_name generates ObjCName annotation`() {
        val source = generateKotlinSource("""
            __attribute__((swift_name("RenamedClass")))
            @interface OriginalClass
            @end
        """.trimIndent())

        assertContainsObjCNameWithSwiftName(source, "RenamedClass")
    }

    @Test
    fun `protocol with swift_name generates ObjCName annotation`() {
        val source = generateKotlinSource("""
            __attribute__((swift_name("SwiftProtocol")))
            @protocol OriginalProtocol
            @end
        """.trimIndent())

        assertContainsObjCNameWithSwiftName(source, "SwiftProtocol")
    }

    @Test
    fun `method with swift_name generates ObjCName annotation`() {
        val source = generateKotlinSource("""
            @interface Foo
            - (void)doSomething __attribute__((swift_name("performAction()")));
            @end
        """.trimIndent())

        assertContainsObjCNameWithSwiftName(source, "performAction()")
    }

    @Test
    fun `method with parameters and swift_name generates ObjCName annotation`() {
        val source = generateKotlinSource("""
            @interface Foo
            - (void)doSomething:(int)value withName:(const char *)name
                __attribute__((swift_name("perform(value:name:)")));
            @end
        """.trimIndent())

        assertContainsObjCNameWithSwiftName(source, "perform(value:name:)")
    }

    @Test
    fun `property with swift_name generates ObjCName annotation`() {
        val source = generateKotlinSource("""
            @interface Foo
            @property int itemCount __attribute__((swift_name("numberOfItems")));
            @end
        """.trimIndent())

        assertContainsObjCNameWithSwiftName(source, "numberOfItems")
    }

    // ==================== Negative Tests ====================

    @Test
    fun `class without swift attributes has no Swift annotations`() {
        val source = generateKotlinSource("""
            @interface PlainClass
            - (void)plainMethod;
            @property int plainProperty;
            @end
        """.trimIndent())

        assertFalse(source.contains("@kotlin.native.ObjCName"),
            "PlainClass should not have @ObjCName annotation:\n$source")
    }

    // ==================== Combined Tests ====================

    @Test
    fun `combined class with multiple swift annotations`() {
        val source = generateKotlinSource("""
            __attribute__((swift_name("DataController")))
            @interface DataManager
            - (void)fetchData __attribute__((swift_name("loadData()")));
            @property (readonly) int itemCount __attribute__((swift_name("numberOfItems")));
            @end
        """.trimIndent())

        assertContainsObjCNameWithSwiftName(source, "DataController")
        assertContainsObjCNameWithSwiftName(source, "loadData()")
        assertContainsObjCNameWithSwiftName(source, "numberOfItems")
    }

    // ==================== SwiftBridge Tests (from API Notes) ====================

    @Test
    fun `class with SwiftBridge from API Notes generates SwiftBridge annotation`() {
        val source = generateKotlinSourceWithApiNotes(
            headerContents = """
                @interface BridgedClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: BridgedClass
                  SwiftBridge: Swift.BridgedValue
            """.trimIndent()
        )

        assertContainsSwiftBridge(source, "Swift.BridgedValue")
    }

    @Test
    fun `class with both SwiftName and SwiftBridge from API Notes`() {
        val source = generateKotlinSourceWithApiNotes(
            headerContents = """
                @interface OriginalClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: OriginalClass
                  SwiftName: RenamedClass
                  SwiftBridge: Swift.BridgedType
            """.trimIndent()
        )

        assertContainsObjCNameWithSwiftName(source, "RenamedClass")
        assertContainsSwiftBridge(source, "Swift.BridgedType")
    }

    @Test
    fun `class without SwiftBridge in API Notes has no SwiftBridge annotation`() {
        val source = generateKotlinSourceWithApiNotes(
            headerContents = """
                @interface PlainApiNotesClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: PlainApiNotesClass
            """.trimIndent()
        )

        assertFalse(source.contains("@kotlin.native.SwiftBridge"),
            "PlainApiNotesClass should not have @SwiftBridge annotation:\n$source")
    }

    // ==================== Helper Methods ====================

    private fun generateKotlinSource(headerContents: String): String {
        val files = testFiles()
        files.file("header.h", headerContents)
        val defFile = files.file("test.def", """
            headers = header.h
            headerFilter = header.h
            language = Objective-C
        """.trimIndent())

        val library = buildNativeLibraryFrom(defFile, files.directory)
        val indexerResult = buildNativeIndex(library, verbose = false)

        return emitKotlinSource(library, indexerResult)
    }

    private fun emitKotlinSource(library: NativeLibrary, indexerResult: IndexerResult): String {
        val configuration = InteropConfiguration(
            library = library,
            pkgName = "test",
            excludedFunctions = emptySet(),
            excludedMacros = emptySet(),
            strictEnums = emptySet(),
            nonStrictEnums = emptySet(),
            noStringConversion = emptySet(),
            exportForwardDeclarations = emptyList(),
            allowedOverloadsForCFunctions = emptySet(),
            disableDesignatedInitializerChecks = false,
            disableExperimentalAnnotation = true,
            target = HostManager.host,
            cCallMode = CCallMode.INDIRECT
        )

        val metadataVersion = KlibMetadataVersion(
            KlibAbiCompatibilityLevel.LATEST_STABLE.toCInteropKlibMetadataVersion().toArray()
        )

        val context = StubIrContext(
            log = { },
            configuration = configuration,
            nativeIndex = indexerResult.index,
            imports = ImportsMock(),
            platform = KotlinPlatform.NATIVE,
            generationMode = GenerationMode.SOURCE_CODE,
            libName = "test",
            allowPrecompiledHeaders = false,
            metadataVersion = metadataVersion
        )

        val builderResult = StubIrBuilder(context).build()
        val bridgeBuilderResult = StubIrBridgeBuilder(context, builderResult).build()

        val output = StringBuilder()
        StubIrTextEmitter(context, builderResult, bridgeBuilderResult, omitEmptyLines = true)
            .emit(output)

        return output.toString()
    }

    private fun generateKotlinSourceWithApiNotes(
        headerContents: String,
        apiNotesContents: String,
        moduleName: String = "TestModule"
    ): String {
        val files = testFiles()

        // Create header file
        files.file("$moduleName.h", headerContents)

        // Create module map
        files.file("module.modulemap", """
            module $moduleName {
              header "$moduleName.h"
              export *
            }
        """.trimIndent())

        // Create API Notes file
        files.file("$moduleName.apinotes", apiNotesContents)

        // Create def file that uses modules with API Notes support
        val defFile = files.file("test.def", """
            language = Objective-C
            modules = $moduleName
            compilerOpts = -fmodules -fapinotes-modules
        """.trimIndent())

        val library = buildNativeLibraryFrom(defFile, files.directory)
        val indexerResult = buildNativeIndex(library, verbose = false)

        return emitKotlinSource(library, indexerResult)
    }

    private fun assertContainsObjCNameWithSwiftName(source: String, expectedSwiftName: String) {
        val pattern = """@kotlin\.native\.ObjCName\([^)]*swiftName\s*=\s*"${Regex.escape(expectedSwiftName)}"[^)]*\)"""
        assertTrue(
            Regex(pattern).containsMatchIn(source),
            "Expected @kotlin.native.ObjCName(swiftName = \"$expectedSwiftName\") in generated source:\n$source"
        )
    }

    private fun assertContainsSwiftBridge(source: String, expectedBridgedType: String) {
        val pattern = """@kotlin\.native\.SwiftBridge\("${Regex.escape(expectedBridgedType)}"\)"""
        assertTrue(
            Regex(pattern).containsMatchIn(source),
            "Expected @kotlin.native.SwiftBridge(\"$expectedBridgedType\") in generated source:\n$source"
        )
    }
}
