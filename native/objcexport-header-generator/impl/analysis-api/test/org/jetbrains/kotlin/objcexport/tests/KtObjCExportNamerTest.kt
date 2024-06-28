/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.bridgeParameter
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.analyzeWithObjCExport
import org.jetbrains.kotlin.psi.KtPsiFactory
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
                symbol.getObjCClassOrProtocolName()
            )
        }
    }

    @Test
    fun `test - class name override`() {
        getSymbol<KaNamedClassSymbol>("class Foo", "Foo", KaScope::classifiers) { symbol ->
            symbol.withOverriddenName("Bar") {
                assertEquals(
                    ObjCExportClassOrProtocolName("Bar", "Bar"),
                    symbol.getObjCClassOrProtocolName()
                )
            }
        }
    }

    @Test
    fun `test - function name override`() {
        getSymbol<KaNamedFunctionSymbol>("fun foo() {}", "foo", KaScope::callables) { symbol ->
            symbol.withOverriddenName("bar") {
                assertEquals(
                    ObjCExportFunctionName("bar", "bar"),
                    symbol.getObjCFunctionName()
                )
            }
        }
    }

    @Test
    fun `test - property name override`() {
        getSymbol<KaPropertySymbol>("var foo: Int", "foo", KaScope::callables) { symbol ->
            symbol.withOverriddenName("bar") {
                assertEquals(
                    ObjCExportPropertyName("bar", "bar"),
                    symbol.getObjCPropertyName()
                )
                assertEquals(
                    ObjCExportFunctionName("bar", "bar"),
                    symbol.getter?.getObjCFunctionName()
                )
                assertEquals(
                    ObjCExportFunctionName("setBar", "setBar"),
                    symbol.setter?.getObjCFunctionName()
                )
            }
        }
    }

    @Test
    fun `test - function signature override`() {
        getSymbol<KaFunctionSymbol>("fun foo(param1: Boolean, param2: String) {}", "foo", KaScope::callables) { symbol ->
            val ktPsiFactory = KtPsiFactory(symbol.psi!!.project)
            val type1 = ktPsiFactory.createTypeCodeFragment("Int", symbol.psi).getContentElement()!!.type
            val type2 = ktPsiFactory.createTypeCodeFragment("Double", symbol.psi).getContentElement()!!.type
            val returnType = ktPsiFactory.createTypeCodeFragment("Float", symbol.psi).getContentElement()!!.type

            val valueParams = listOf(
                type1.bridgeParameter() to KtObjCParameterData(Name.identifier("intParam"), false, type1, false),
                type2.bridgeParameter() to KtObjCParameterData(Name.identifier("doubleParam"), false, type2, false)
            )

            symbol.withOverriddenSignature("bar", returnType, valueParams) {
                val objCMethod = symbol.translateToObjCMethod()!!

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

    @Suppress("CONTEXT_RECEIVERS_DEPRECATED")
    private inline fun <reified T> getSymbol(
        @Language("kotlin") sourceCode: String,
        name: String,
        symbolsGetter: KaScope.(name: Name) -> Sequence<*>,
        action: context(KaSession, KtObjCExportSession) (T) -> Unit,
    ) {
        val file = inlineSourceCodeAnalysis.createKtFile(sourceCode)
        analyzeWithObjCExport(file) {
            val symbol = file.symbol.fileScope.symbolsGetter(Name.identifier(name)).single() as T
            action(useSiteSession, useSiteExportSession, symbol)
        }
    }
}
