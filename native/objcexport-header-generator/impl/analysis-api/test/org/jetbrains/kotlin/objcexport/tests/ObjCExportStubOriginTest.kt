/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.testUtils.analyzeWithObjCExport
import org.jetbrains.kotlin.objcexport.testUtils.createObjCExportFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ObjCExportStubOriginTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    // Tests for scenarios covered in KT-74700
    @Test
    fun `test - generated accessors have same origin as interface for object`() {
        inlineSourceCodeAnalysis.createObjCExportFile(
            """
                object SomeObject {
                    const val x = 42
                }
            """.trimIndent()
        ) { file ->
            val classSymbols = file.classifierSymbols
            val objectSymbol = classSymbols.single()
            val objectStub = translateToObjCExportStub(objectSymbol)
            assertIs<ObjCInterface>(objectStub, "Object stub should be an interface")

            val sharedProperty = objectStub.members.firstOrNull {
                it is ObjCProperty && it.name == ObjCPropertyNames.objectPropertyName
            }
            assertNotNull(sharedProperty, "Object stub should have a `shared` property")

            val namedAccessor = objectStub.members.firstOrNull {
                it is ObjCMethod && it.name == "someObject"
            }
            assertNotNull(namedAccessor, "Object stub should have a named accessor")

            assertIs<ObjCExportStubOrigin.Source>(
                objectStub.origin,
                "Object stub should have a source origin"
            )

            assertEquals(
                objectStub.origin,
                sharedProperty.origin,
                "Object class and its shared property should have the same origin"
            )

            assertEquals(
                objectStub.origin,
                namedAccessor.origin,
                "Object class and its named accessor should have the same origin"
            )
        }
    }

    @Test
    fun `test - generated accessors have same origin as interface for companion object`() {
        inlineSourceCodeAnalysis.createObjCExportFile(
            """
                class SomeClass {
                    companion object {
                        val x = 42
                    }
                }
            """.trimIndent()
        ) { file ->
            val classSymbols = file.classifierSymbols
            val companionObjectSymbol = classSymbols.firstOrNull {
                it.classKind == KaClassKind.COMPANION_OBJECT
            }
            assertNotNull(companionObjectSymbol, "Companion object symbol should be present")

            val classSymbol = classSymbols.firstOrNull {
                it.classKind == KaClassKind.CLASS
            }
            assertNotNull(classSymbol, "Class symbol should be present")

            val companionObjectStub = translateToObjCExportStub(companionObjectSymbol)
            assertIs<ObjCInterface>(companionObjectStub, "Object stub should be an interface")

            val classStub = translateToObjCExportStub(classSymbol)
            assertIs<ObjCInterface>(classStub, "Class stub should be an interface")

            val sharedProperty = companionObjectStub.members.firstOrNull {
                it is ObjCProperty && it.name == ObjCPropertyNames.objectPropertyName
            }
            assertNotNull(sharedProperty, "Companion object stub should have a `shared` property")

            val companionProperty = classStub.members.firstOrNull {
                it is ObjCProperty && it.name == ObjCPropertyNames.companionObjectPropertyName
            }
            assertNotNull(companionProperty, "Class stub should have a `companion` property")

            val origin = companionObjectStub.origin
            assertIs<ObjCExportStubOrigin.Source>(
                origin,
                "Companion object stub should have a source origin"
            )

            assertEquals(
                origin,
                sharedProperty.origin,
                "Shared property should have the same origin as its class"
            )

            assertEquals(
                origin,
                companionProperty.origin,
                "Class companion property should has the same origin as its companion class"
            )
        }
    }


    // Test for the scenario covered in KT-74721
    @Test
    fun `test - top level function wrapper has origin pointing to kotlin file psi`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                fun someGlobalFunction() {
                    println("Hello, world!")
                }
            """.trimIndent()
        )

        analyzeWithObjCExport(file) {
            val header = translateToObjCHeader(listOf(KtObjCExportFile(file)))
            val stub = header.stubs.firstOrNull {
                it.name.endsWith("Kt") // Top level facade has a name ending with "Kt"
            }

            assertNotNull(stub, "Top level facade should be present")

            val origin = stub.origin
            assertIs<ObjCExportStubOrigin.Source>(origin, "Top level facade should have a source origin")
            assertEquals(file, origin.psi, "Top level facade origin should point to the kotlin file psi")
        }
    }
}
