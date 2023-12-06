/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.descriptors.enumEntries
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer.ClassOrProtocolName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer.PropertyName
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.scopes.findFirstVariable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

/**
 * ## Test Scope
 * This test will cover basic cases of the [ObjCExportNamer]. <br>
 *
 * The __input__ to this test will be descriptors defined as 'inline source code'.
 * (e.g. a String like "class Foo").<br>
 *
 * The __output__ of this test will be result of the request to the [ObjCExportNamer].
 * (e.g. the [ClassOrProtocolName])
 */
class ObjCExportNamerTest : InlineSourceTestEnvironment {

    override val testDisposable = Disposer.newDisposable("${ObjCExportNamerTest::class.simpleName}.testDisposable")

    override val kotlinCoreEnvironment = createKotlinCoreEnvironment(testDisposable)

    @TempDir
    override lateinit var testTempDir: File

    @AfterEach
    fun dispose() {
        Disposer.dispose(testDisposable)
    }

    @Test
    fun `test - simple class`() {
        val module = createModuleDescriptor("class Foo")
        val clazz = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        assertEquals(
            ClassOrProtocolName("Foo", "Foo"),
            createObjCExportNamer().getClassOrProtocolName(clazz)
        )
    }

    @Test
    fun `test - simple class - with prefix`() {
        val module = createModuleDescriptor("class Foo")
        val clazz = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        assertEquals(
            ClassOrProtocolName("Foo", "XFoo"),
            createObjCExportNamer(createObjCExportNamerConfiguration(topLevelNamePrefix = "X")).getClassOrProtocolName(clazz)
        )
    }

    @Test
    fun `test - simple function`() {
        val module = createModuleDescriptor("fun foo() = 42")
        val foo = module.getPackage(FqName.ROOT).memberScope.findSingleFunction(Name.identifier("foo"))
        assertEquals("foo()", createObjCExportNamer().getSwiftName(foo))
        assertEquals("foo", createObjCExportNamer().getSelector(foo))
    }

    @Test
    fun `test - function with parameters`() {
        val module = createModuleDescriptor("fun foo(a: Int, b: Int) = a + b")
        val foo = module.getPackage(FqName.ROOT).memberScope.findSingleFunction(Name.identifier("foo"))
        assertEquals("foo(a:b:)", createObjCExportNamer().getSwiftName(foo))
        assertEquals("fooA:b:", createObjCExportNamer().getSelector(foo))
    }

    @Test
    fun `test - simple property`() {
        val module = createModuleDescriptor("val foo = 42")
        val foo = module.getPackage(FqName.ROOT).memberScope.findFirstVariable("foo") { true }!!
        assertEquals(PropertyName("foo", "foo"), createObjCExportNamer().getPropertyName(foo))
    }

    @Test
    fun `test - simple property - with prefix`() {
        val module = createModuleDescriptor("val foo = 42")
        val foo = module.getPackage(FqName.ROOT).memberScope.findFirstVariable("foo") { true }!!
        assertEquals(
            PropertyName("foo", "foo"),
            createObjCExportNamer(createObjCExportNamerConfiguration(topLevelNamePrefix = "X")).getPropertyName(foo)
        )
    }

    @Test
    fun `test - function inside class`() {
        val module = createModuleDescriptor(
            """
            package bar
            class Foo {
                fun someFunction(a: Int, b: Int) = a + b
            }
            """.trimIndent()
        )

        val fooClass = module.findClassAcrossModuleDependencies(ClassId.fromString("bar/Foo"))!!
        val someFunction = fooClass.unsubstitutedMemberScope.findSingleFunction(Name.identifier("someFunction"))
        assertEquals("someFunction(a:b:)", createObjCExportNamer().getSwiftName(someFunction))
        assertEquals("someFunctionA:b:", createObjCExportNamer().getSelector(someFunction))
    }

    @Test
    fun `test - class with ObjCName annotation`() {
        val module = createModuleDescriptor(
            """
            @kotlin.native.ObjCName("ObjCFoo", "SwiftFoo")
            class Foo
        """.trimIndent()
        )

        val fooClass = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        assertEquals(
            ClassOrProtocolName(swiftName = "SwiftFoo", objCName = "ObjCFoo"),
            createObjCExportNamer().getClassOrProtocolName(fooClass)
        )
    }

    @Test
    fun `test - simple parameter`() {
        val module = createModuleDescriptor("fun foo(a: Int)")
        val foo = module.getPackage(FqName.ROOT).memberScope.findSingleFunction(Name.identifier("foo"))
        val parameterA = foo.valueParameters.find { it.name == Name.identifier("a") }!!
        assertEquals("a", createObjCExportNamer().getParameterName(parameterA))
    }

    @Test
    fun `test - parameter with ObjCName annotation`() {
        val module = createModuleDescriptor(
            """
            import kotlin.native.ObjCName
            fun foo(@ObjCName("aObjC", "aSwift") a: Int)
        """.trimIndent()
        )

        val foo = module.getPackage(FqName.ROOT).memberScope.findSingleFunction(Name.identifier("foo"))
        val parameterA = foo.valueParameters.find { it.name == Name.identifier("a") }!!
        assertEquals("aObjC", createObjCExportNamer().getParameterName(parameterA))
    }

    @Test
    fun `test - class mangling`() {
        val module = createModuleDescriptor {
            source(
                """
                package a 
                class Foo
            """.trimIndent()
            )
            source(
                """
                package b
                class Foo
            """.trimIndent()
            )
        }

        val aFoo = module.findClassAcrossModuleDependencies(ClassId.fromString("a/Foo"))!!
        val bFoo = module.findClassAcrossModuleDependencies(ClassId.fromString("b/Foo"))!!

        val namer = createObjCExportNamer()
        assertEquals(ClassOrProtocolName("Foo", "Foo"), namer.getClassOrProtocolName(aFoo))
        assertEquals(ClassOrProtocolName("Foo_", "Foo_"), namer.getClassOrProtocolName(bFoo))

        /* Check caching in namer: Should return same name again */
        assertEquals(ClassOrProtocolName("Foo", "Foo"), namer.getClassOrProtocolName(aFoo))
    }

    @Test
    fun `test - simple enum`() {
        val module = createModuleDescriptor(
            """
            enum class Foo {
                A, B, C
            }
        """.trimIndent()
        )

        val foo = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        val fooA = foo.enumEntries.find { it.name == Name.identifier("A") }!!
        val namer = createObjCExportNamer()

        assertEquals(ClassOrProtocolName("Foo", "Foo"), namer.getClassOrProtocolName(foo))
        assertEquals("a", namer.getEnumEntrySelector(fooA))
        assertEquals("a", namer.getEnumEntrySwiftName(fooA))
    }
}
