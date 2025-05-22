/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IndexProtocols : IndexerTests() {

    private val moduleName = "Foo"
    private var files: TempFiles = TempFiles(moduleName)

    companion object {
        private val appleSdkPath = "/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX15.4.sdk"
        private val appleFrameworkPath = "$appleSdkPath/System/Library/Frameworks"

        private val xcodeDir = "/Applications/Xcode.app/Contents/Developer"
        private val clangVersion = "17.0.0"
        private val clangResourceDir = "$xcodeDir/Toolchains/XcodeDefault.xctoolchain/usr/lib/clang/$clangVersion/include"
    }

    @Before
    fun before() {
        files = TempFiles(moduleName)
    }

    @Test
    fun `generate swift simple method call`() {
        val fooHeader = files.file("Foo.h", """
            __attribute__((swift_name("SharedFoo")))
            @interface Foo
            + (instancetype)init __attribute__((swift_name("init()")));
            - (void)bar __attribute__((swift_name("bar()")));
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(fooHeader), files)
        val foo = indexerResult.index.objCClasses.first { it.name == "Foo" }
        val res = buildSwiftApiCall(foo).trimIndent()
        assertEquals("""
            let sharedFoo_0 = SharedFoo.init()
            sharedFoo_0.bar()
        """.trimIndent(), res)
    }

    @Test
    fun `generate swift empty constructor call`() {
        val fooHeader = files.file("Foo.h", """
            __attribute__((swift_name("SharedFoo")))
            @interface Foo
            + (instancetype)init __attribute__((swift_name("init()")));
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(fooHeader), files)
        val foo = indexerResult.index.objCClasses.first { it.name == "Foo" }
        val res = buildSwiftApiCall(foo).trimIndent()
        assertEquals("""
            let sharedFoo_0 = SharedFoo.init()
        """.trimIndent(), res)
    }

    @Test
    fun `generate swift constructor call with parameter`() {
        val fooHeader = files.file("Foo.h", """
            
            #import <Foundation/NSValue.h>
            
            @interface Foo
            - (instancetype)initWithN:(int32_t)n __attribute__((swift_name("init(n:)"))) __attribute__((objc_designated_initializer));
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(
                listOf(fooHeader), files,
                "-isysroot", appleSdkPath
        )
        val foo = indexerResult.index.objCClasses.first { it.name == "Foo" }
        val res = buildSwiftApiCall(foo).trimIndent().trim()
        assertEquals("""
            let foo_0 = Foo.init(n: 42)
        """.trimIndent(), res)
    }

    @Test
    fun `read class or protocol swift_name attr`() {
        val fooHeader = files.file("Foo.h", """
            __attribute__((swift_name("Bar")))
            @protocol Foo
            @end
            
            __attribute__((swift_name("Foo")))
            @interface Bar
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(fooHeader), files)
        val foo = indexerResult.index.objCProtocols.first { it.name == "Foo" }
        val bar = indexerResult.index.objCClasses.first { it.name == "Bar" }
        assertEquals("Bar", foo.swiftName)
        assertEquals("Foo", bar.swiftName)
    }

    @Test
    fun `read property swift_name attr`() {
        val fooHeader = files.file("Foo.h", """
            @interface Foo
            @property (readonly) void bar __attribute__((swift_name("swiftBar")));
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(fooHeader), files)
        val foo = indexerResult.index.objCClasses.first { it.name == "Foo" }
        val prop = foo.properties.first { it.name == "bar" }
        assertEquals("swiftBar", prop.swiftName)
    }

    @Test
    fun `read method swift_name attr`() {
        val fooHeader = files.file("Foo.h", """
            @protocol Foo
            - (void)hasNext __attribute__((swift_name("swiftHasNext()")));
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(fooHeader), files)
        val foo = indexerResult.index.objCProtocols.first { it.name == "Foo" }
        val method = foo.methods.first { it.selector == "hasNext" }
        assertEquals("swiftHasNext()", method.swiftName)
    }

    @Test
    fun `basic protocol`() {
        val fooHeader = files.file("Foo.h", """
            @protocol Foo
            - (void)doSomething;
            @end
        """.trimIndent())

        val indexerResult = compileAndIndex(listOf(fooHeader), files)
        val foo = indexerResult.index.objCProtocols.first()

        assertEquals(1, indexerResult.index.objCProtocols.size)
        assertEquals(1, foo.methods.size)
        assertEquals("doSomething", foo.methods.first().selector)
    }

    @Test
    fun `kotlin array`() {

        val arrayHeader = files.file("KotlinArray.h", """

            #import <Foundation/NSArray.h>
            #import <Foundation/NSDictionary.h>
            #import <Foundation/NSError.h>
            #import <Foundation/NSObject.h>
            #import <Foundation/NSSet.h>
            #import <Foundation/NSString.h>
            #import <Foundation/NSValue.h>
            
            @protocol KotlinIterator
            @required
            - (BOOL)hasNext __attribute__((swift_name("hasNext()")));
            - (id _Nullable)next __attribute__((swift_name("next()")));
            @end
            
            __attribute__((objc_subclassing_restricted))
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


        val base = files.file("Base.h", kotlinCinteropTypes.trimIndent())

        val index = compileAndIndex(
                listOf(base, arrayHeader),
                files,
                "-isysroot", appleSdkPath,
                "-F", appleFrameworkPath,
        ).index

        val kotlinArray = index.objCClasses.first { it.name == "KotlinArray" }
        val getIndex = kotlinArray.methods.firstOrNull { it.selector == "getIndex:" }

        assertNotNull(getIndex)
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
                "-I$clangResourceDir",
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

private const val kotlinCinteropTypes = """
    
    #ifndef CINTEROP_SHIMS_H
    #define CINTEROP_SHIMS_H
    
    #import <Foundation/NSArray.h>
    #import <Foundation/NSDictionary.h>
    #import <Foundation/NSError.h>
    #import <Foundation/NSObject.h>
    #import <Foundation/NSSet.h>
    #import <Foundation/NSString.h>
    #import <Foundation/NSValue.h>
    
    __attribute__((swift_name("KotlinBase")))
    @interface Base : NSObject
    - (instancetype)init __attribute__((unavailable));
    + (instancetype)new __attribute__((unavailable));
    + (void)initialize __attribute__((objc_requires_super));
    @end
    @interface Base (BaseCopying) <NSCopying>
    @end
    __attribute__((swift_name("KotlinMutableSet")))
    @interface MutableSet<ObjectType> : NSMutableSet<ObjectType>
    @end
    __attribute__((swift_name("KotlinMutableDictionary")))
    @interface MutableDictionary<KeyType, ObjectType> : NSMutableDictionary<KeyType, ObjectType>
    @end
    @interface NSError (NSErrorKotlinException)
    @property (readonly) id _Nullable kotlinException;
    @end
    __attribute__((swift_name("KotlinNumber")))
    @interface Number : NSNumber
    - (instancetype)initWithChar:(char)value __attribute__((unavailable));
    - (instancetype)initWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
    - (instancetype)initWithShort:(short)value __attribute__((unavailable));
    - (instancetype)initWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
    - (instancetype)initWithInt:(int)value __attribute__((unavailable));
    - (instancetype)initWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
    - (instancetype)initWithLong:(long)value __attribute__((unavailable));
    - (instancetype)initWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
    - (instancetype)initWithLongLong:(long long)value __attribute__((unavailable));
    - (instancetype)initWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
    - (instancetype)initWithFloat:(float)value __attribute__((unavailable));
    - (instancetype)initWithDouble:(double)value __attribute__((unavailable));
    - (instancetype)initWithBool:(BOOL)value __attribute__((unavailable));
    - (instancetype)initWithInteger:(NSInteger)value __attribute__((unavailable));
    - (instancetype)initWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
    + (instancetype)numberWithChar:(char)value __attribute__((unavailable));
    + (instancetype)numberWithUnsignedChar:(unsigned char)value __attribute__((unavailable));
    + (instancetype)numberWithShort:(short)value __attribute__((unavailable));
    + (instancetype)numberWithUnsignedShort:(unsigned short)value __attribute__((unavailable));
    + (instancetype)numberWithInt:(int)value __attribute__((unavailable));
    + (instancetype)numberWithUnsignedInt:(unsigned int)value __attribute__((unavailable));
    + (instancetype)numberWithLong:(long)value __attribute__((unavailable));
    + (instancetype)numberWithUnsignedLong:(unsigned long)value __attribute__((unavailable));
    + (instancetype)numberWithLongLong:(long long)value __attribute__((unavailable));
    + (instancetype)numberWithUnsignedLongLong:(unsigned long long)value __attribute__((unavailable));
    + (instancetype)numberWithFloat:(float)value __attribute__((unavailable));
    + (instancetype)numberWithDouble:(double)value __attribute__((unavailable));
    + (instancetype)numberWithBool:(BOOL)value __attribute__((unavailable));
    + (instancetype)numberWithInteger:(NSInteger)value __attribute__((unavailable));
    + (instancetype)numberWithUnsignedInteger:(NSUInteger)value __attribute__((unavailable));
    @end
    __attribute__((swift_name("KotlinUByte")))
    @interface UByte : Number
    - (instancetype)initWithUnsignedChar:(unsigned char)value;
    + (instancetype)numberWithUnsignedChar:(unsigned char)value;
    @end
    __attribute__((swift_name("KotlinShort")))
    @interface Short : Number
    - (instancetype)initWithShort:(short)value;
    + (instancetype)numberWithShort:(short)value;
    @end
    __attribute__((swift_name("KotlinUShort")))
    @interface UShort : Number
    - (instancetype)initWithUnsignedShort:(unsigned short)value;
    + (instancetype)numberWithUnsignedShort:(unsigned short)value;
    @end
    __attribute__((swift_name("KotlinInt")))
    @interface Int : Number
    - (instancetype)initWithInt:(int)value;
    + (instancetype)numberWithInt:(int)value;
    @end
    __attribute__((swift_name("KotlinUInt")))
    @interface UInt : Number
    - (instancetype)initWithUnsignedInt:(unsigned int)value;
    + (instancetype)numberWithUnsignedInt:(unsigned int)value;
    @end
    __attribute__((swift_name("KotlinLong")))
    @interface Long : Number
    - (instancetype)initWithLongLong:(long long)value;
    + (instancetype)numberWithLongLong:(long long)value;
    @end
    __attribute__((swift_name("KotlinULong")))
    @interface ULong : Number
    - (instancetype)initWithUnsignedLongLong:(unsigned long long)value;
    + (instancetype)numberWithUnsignedLongLong:(unsigned long long)value;
    @end
    __attribute__((swift_name("KotlinFloat")))
    @interface Float : Number
    - (instancetype)initWithFloat:(float)value;
    + (instancetype)numberWithFloat:(float)value;
    @end
    __attribute__((swift_name("KotlinDouble")))
    @interface Double : Number
    - (instancetype)initWithDouble:(double)value;
    + (instancetype)numberWithDouble:(double)value;
    @end

    #endif // CINTEROP_SHIMS_H
"""