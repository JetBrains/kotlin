/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.testUtils.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeAttributes
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * ## Test Scope
 * This test shall cover test the [ObjCExportMapper];
 * The test will invoke the type mapping a known type (e.g. List<Int>) and check
 * the corresponding conversion to [ObjCNonNullReferenceType].
 *
 * The test also acts as quick entry point for debugging the [ObjCExportMapper]
 */
class ObjCExportMapperTest : InlineSourceTestEnvironment {
    override val testDisposable = Disposer.newDisposable("${ObjCExportMapperTest::class.simpleName}.testDisposable")
    override val kotlinCoreEnvironment: KotlinCoreEnvironment = createKotlinCoreEnvironment(testDisposable)

    @TempDir
    override lateinit var testTempDir: File

    @AfterEach
    fun dispose() {
        Disposer.dispose(testDisposable)
    }

    /**
     * No 'type mapper' expected for a simple Kotlin class like 'Foo'.
     * Only well known standard types (List, ...) will get mapped to their corresponding
     * ObjC counterpart (NSArray, ...)
     */
    @Test
    fun `test - simple class`() {
        val module = createModuleDescriptor("class Foo")
        val foo = module.findClassAcrossModuleDependencies(ClassId.fromString("Foo"))!!
        assertNull(createObjCExportMapper().getCustomTypeMapper(foo))
    }

    /**
     * Will test ObjC type mapping from List<Int> to NSArray<Int *> *
     */
    @Test
    fun `test - List of int`() {
        /* We are only using built in / stdlib parts, so we can provide empty source code */
        val module = createModuleDescriptor("")
        val objcExportMapper = createObjCExportMapper()
        val objcExportNamer = createObjCExportNamer(mapper = objcExportMapper)

        val objcExportTranslator = ObjCExportTranslatorImpl(
            generator = ObjCExportHeaderGeneratorImpl(
                moduleDescriptors = listOf(module),
                mapper = objcExportMapper,
                namer = objcExportNamer,
                problemCollector = ObjCExportProblemCollector.SILENT,
                objcGenerics = true,
                shouldExportKDoc = false,
                additionalImports = emptyList()
            ),
            mapper = objcExportMapper,
            namer = objcExportNamer,
            problemCollector = ObjCExportProblemCollector.SILENT,
            objcGenerics = true
        )

        val listClassDescriptor = module.findClassAcrossModuleDependencies(ClassId.fromString("kotlin/collections/List"))!!
        val intClassDescriptor = module.findClassAcrossModuleDependencies(ClassId.fromString("kotlin/Int"))!!

        /* Represents List<Int> */
        val listOfIntType = KotlinTypeFactory.simpleNotNullType(
            TypeAttributes.Empty, listClassDescriptor, listOf(
                TypeProjectionImpl(KotlinTypeFactory.simpleNotNullType(TypeAttributes.Empty, intClassDescriptor, emptyList()))
            )
        )

        val typeMapper = assertNotNull(objcExportMapper.getCustomTypeMapper(listClassDescriptor))
        assertEquals(ClassId.fromString("kotlin/collections/List"), typeMapper.mappedClassId)
        val listOfIntMapped = typeMapper.mapType(listOfIntType, objcExportTranslator, objCExportScope = ObjCRootExportScope)

        assertEquals(ObjCClassType("NSArray", typeArguments = listOf(ObjCClassType("Int"))), listOfIntMapped)
        assertEquals("NSArray<Int *> *", listOfIntMapped.toString())
    }
}
