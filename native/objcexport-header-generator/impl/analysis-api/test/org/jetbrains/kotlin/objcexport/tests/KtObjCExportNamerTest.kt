/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFunctionName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportPropertyName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCPrimitiveType
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.bridgeParameter
import org.jetbrains.kotlin.objcexport.testUtils.analyzeWithObjCExport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KtObjCExportNamerTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - simple class`() {
        getSymbol<KaNamedClassSymbol>("class Foo", "Foo", KaScope::classifiers) { symbol ->
            assertEquals(
                ObjCExportClassOrProtocolName("Foo", "Foo"),
                getObjCClassOrProtocolName(symbol)
            )
        }
    }

    @Test
    fun `test - class name override`() {
        getSymbol<KaNamedClassSymbol>("class Foo", "Foo", KaScope::classifiers) { symbol ->
            withOverriddenName(symbol, "Bar") {
                assertEquals(
                    ObjCExportClassOrProtocolName("Bar", "Bar"),
                    getObjCClassOrProtocolName(symbol)
                )
            }
        }
    }

    @Test
    fun `test - function name override`() {
        getSymbol<KaNamedFunctionSymbol>("fun foo() {}", "foo", KaScope::callables) { symbol ->
            withOverriddenName(symbol, "bar") {
                assertEquals(
                    ObjCExportFunctionName("bar", "bar"),
                    getObjCFunctionName(symbol)
                )
            }
        }
    }

    @Test
    fun `test - property name override`() {
        getSymbol<KaPropertySymbol>("var foo: Int", "foo", KaScope::callables) { symbol ->
            withOverriddenName(symbol, "bar") {
                assertEquals(
                    ObjCExportPropertyName("bar", "bar"),
                    getObjCPropertyName(symbol)
                )
                assertEquals(
                    ObjCExportFunctionName("bar", "bar"),
                    getObjCFunctionName(symbol.getter!!)
                )
                assertEquals(
                    ObjCExportFunctionName("setBar", "setBar"),
                    getObjCFunctionName(symbol.setter!!)
                )
            }
        }
    }

    @Test
    fun `test - function signature override`() {
        getSymbol<KaFunctionSymbol>("fun foo(param1: Boolean, param2: String) {}", "foo", KaScope::callables) { symbol ->
            val type1 = with(analysisSession) { buildClassType(StandardClassIds.Int) }
            val type2 = with(analysisSession) { buildClassType(StandardClassIds.Double) }
            val returnType = with(analysisSession) { buildClassType(StandardClassIds.Float) }

            val valueParams = listOf(
                bridgeParameter(type1) to KtObjCParameterData(Name.identifier("intParam"), false, type1, false),
                bridgeParameter(type2) to KtObjCParameterData(Name.identifier("doubleParam"), false, type2, false)
            )

            withOverriddenSignature(symbol, "bar", returnType, valueParams) {

                val objCMethod = translateToObjCMethod(symbol)!!

                assertEquals("barIntParam:doubleParam:", objCMethod.name)
                assertEquals(ObjCPrimitiveType.float, objCMethod.returnType)

                assertEquals("intParam", objCMethod.parameters[0].name)
                assertEquals(ObjCPrimitiveType.int32_t, objCMethod.parameters[0].type)
                assertEquals("doubleParam", objCMethod.parameters[1].name)
                assertEquals(ObjCPrimitiveType.double, objCMethod.parameters[1].type)

                assertEquals("swift_name(\"bar(intParam:doubleParam:)\")", objCMethod.attributes[0])
            }
        }
    }

    private inline fun <reified T> getSymbol(
        @Language("kotlin") sourceCode: String,
        name: String,
        symbolsGetter: KaScope.(name: Name) -> Sequence<*>,
        action: ObjCExportContext.(T) -> Unit,
    ) {
        val file = inlineSourceCodeAnalysis.createKtFile(sourceCode)
        analyzeWithObjCExport(file) {
            val symbol = with(analysisSession) { file.symbol.fileScope.symbolsGetter(Name.identifier(name)).single() as T }
            action(this, symbol)
        }
    }
}
