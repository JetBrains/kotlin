/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class IndexProtocols : IndexerTests() {

    private val moduleName = "Foo"
    private var files: TempFiles = TempFiles(moduleName)
    private val appleSdkPath = "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX15.4.sdk"
    private val appleFrameworkPath = "$appleSdkPath/System/Library/Frameworks"

    @Before
    fun before() {
        files = TempFiles(moduleName)
    }

    @Test
    fun `basic protocol`() {
        val arrayHeader = files.file("Foo.h", """
            @protocol Foo
            - (void)doSomething;
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(arrayHeader), files)
        val foo = indexerResult.index.objCProtocols.first()

        assertEquals(1, indexerResult.index.objCProtocols.size)
        assertEquals(1, foo.methods.size)
        assertEquals("doSomething", foo.methods.first().selector)
    }

    @Test
    fun `kotlin array`() {

        val arrayHeader = files.file("Foo.h", """

            #import <Foundation/NSArray.h>
            #import <Foundation/NSDictionary.h>
            #import <Foundation/NSError.h>
            #import <Foundation/NSObject.h>
            #import <Foundation/NSSet.h>
            #import <Foundation/NSString.h>
            #import <Foundation/NSValue.h>
            #include <Base.h>
                     
            @interface KotlinArray<T> : Base
            + (instancetype)arrayWithSize:(int32_t)size init:(T _Nullable (^)(Int *))init __attribute__((swift_name("init(size:init:)")));
            + (instancetype)alloc __attribute__((unavailable));
            + (instancetype)allocWithZone:(struct _NSZone *)zone __attribute__((unavailable));
            - (T _Nullable)getIndex:(int32_t)index __attribute__((swift_name("get(index:)")));
            - (id<KotlinIterator>)iterator __attribute__((swift_name("iterator()")));
            - (void)setIndex:(int32_t)index value:(T _Nullable)value __attribute__((swift_name("set(index:value:)")));
            @property (readonly) int32_t size __attribute__((swift_name("size")));
            @end
        """.trimIndent())

        val base = files.file("Base.h", """
            @interface Base
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(
                listOf(base, arrayHeader),
                files,
                "-isysroot", appleSdkPath,
                "-F", appleFrameworkPath,
                "-fmodules",
                "-fcxx-modules"
        )

        assertEquals(1, indexerResult.index.objCProtocols.size)

        val prot = indexerResult.index.objCProtocols.first()
        prot.methods.forEach {
            println(it)
        }
    }

    private fun compileAndIndex(
            headers: List<File>,
            files: TempFiles,
            vararg args: String
    ): IndexerResult {

        val headersNames = headers.map {
            "header \"" + it.name + "\"\n"
        }

        files.file("module.modulemap", """
            module Foo {
              ${headersNames.joinToString(separator = "")}
            }
        """.trimIndent())

        val includeInfos = headers.map {
            IncludeInfo(it.absolutePath, moduleName)
        }

        val compilation = compilation(
                includeInfos,
                "-I${files.directory}",
                *args
        )

        val nativeLibrary = NativeLibrary(
                includes = compilation.includes,
                additionalPreambleLines = compilation.additionalPreambleLines,
                compilerArgs = compilation.compilerArgs,
                headerToIdMapper = HeaderToIdMapper(sysRoot = ""),
                language = compilation.language,
                excludeSystemLibs = false,
                headerExclusionPolicy = HeaderExclusionPolicyImpl(),
                headerFilter = NativeLibraryHeaderFilter.Predefined(
                        files.directory.listFiles()?.filter { it.extension == "h" }?.map { it.path }.orEmpty().toSet(), listOf("*")
                ),
                objCClassesIncludingCategories = emptySet(),
                allowIncludingObjCCategoriesFromDefFile = false
        )

        return buildNativeIndex(nativeLibrary, verbose = true)
    }

    private fun compilation(includes: List<IncludeInfo>, vararg args: String) = CompilationImpl(
            includes = includes,
            additionalPreambleLines = emptyList(),
            compilerArgs = listOf(*args),
            language = Language.OBJECTIVE_C
    )
}

class HeaderExclusionPolicyImpl : HeaderExclusionPolicy {
    override fun excludeAll(headerId: HeaderId): Boolean = false
}