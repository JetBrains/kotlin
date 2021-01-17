/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

internal data class CCalleeWrapper(val lines: List<String>)

/**
 * Some functions don't have an address (e.g. macros-based or builtins).
 * To solve this problem we generate a wrapper function.
 */
internal class CWrappersGenerator(private val context: StubIrContext) {

    private var currentFunctionWrapperId = 0

    private val packageName =
            context.configuration.pkgName.replace(INVALID_CLANG_IDENTIFIER_REGEX, "_")

    private fun generateFunctionWrapperName(functionName: String): String {
        return "${packageName}_${functionName}_wrapper${currentFunctionWrapperId++}"
    }

    private fun bindSymbolToFunction(symbol: String, function: String): List<String> =
        if (context.configuration.library.language == Language.CPP) {
            listOf(
                "extern \"C\" const void* $symbol __asm(${symbol.quoteAsKotlinLiteral()});",
                "extern \"C\" const void* $symbol = (const void*)&$function;"
            )
        } else {
            listOf(
                "const void* $symbol __asm(${symbol.quoteAsKotlinLiteral()});",
                "const void* $symbol = &$function;"
            )
        }

    private data class Parameter(val type: String, val name: String)

    private fun createWrapper(
            symbolName: String,
            wrapperName: String,
            returnType: String,
            parameters: List<Parameter>,
            body: String
    ): List<String> = listOf(
            "__attribute__((always_inline))",
            "$returnType $wrapperName(${parameters.joinToString { "${it.type} ${it.name}" }}) {",
            "\t$body",
            "}",
            *bindSymbolToFunction(symbolName, wrapperName).toTypedArray()
    )

    fun generateCCalleeWrapper(function: FunctionDecl, symbolName: String): CCalleeWrapper =
            if (function.isVararg) {
                CCalleeWrapper(bindSymbolToFunction(symbolName, function.name))
            } else {
                val wrapperName = generateFunctionWrapperName(function.name)

                val returnType = function.returnType.getStringRepresentation()
                val unwrappedReturnType = function.returnType.unwrapTypedefs()
                val returnTypePrefix =
                    if (unwrappedReturnType is PointerType && unwrappedReturnType.isLVReference) "&" else ""
                val returnTypePostfix =
                    if (unwrappedReturnType is ManagedType) ".release()" else ""

                val signatureParameters = function.parameters.mapIndexed { index, parameter ->
                    val type = parameter.type.getStringRepresentation()
                    val forcePointer = if (parameter.type.unwrapTypedefs() is RecordType) "*" else ""
                    Parameter(type+forcePointer, "p$index")
                }
                val bodyParameters = function.parameters.mapIndexed { index, parameter ->

                    val parameterTypeText = parameter.type.getStringRepresentation()
                    val type = parameter.type
                    val unwrappedType = type.unwrapTypedefs()

                    val typeExpression = if (context.configuration.library.language == Language.CPP) {
                        val cppRefTypePrefix =
                            if (unwrappedType is PointerType && unwrappedType.isLVReference) "*" else ""
                        when {
                            type is Typedef ->
                                "(${type.def.name})"
                            type is PointerType && type.spelling != null ->
                                "(${type.spelling})$cppRefTypePrefix"
                            unwrappedType is EnumType ->
                                "(${unwrappedType.def.spelling})"
                            unwrappedType is RecordType ->
                                "*(${unwrappedType.decl.spelling}*)"
                            unwrappedType is ManagedType -> {
                                "sk_ref_sp<${unwrappedType.decl.stripSkiaSharedPointer}>"
                        }
                        else ->
                                "$cppRefTypePrefix($parameterTypeText)"
                        }
                    } else "($parameterTypeText)"

                    Parameter(typeExpression, "p$index")
                }

                // val callExpression = "${function.name}(${parameters.joinToString { it.name }});"
                val callExpression = with (function) {
                    when  {
                        isCxxInstanceMethod -> {
                            val parametersPart = bodyParameters.drop(1).joinToString {
                                "${it.type}${it.name}"
                            }
                            "(${bodyParameters[0].name})->${name}($parametersPart)"
                        }
                        isCxxConstructor -> {
                            val parametersPart = bodyParameters.drop(1).joinToString {
                                "${it.type}${it.name}"
                            }
                            "new(${bodyParameters[0].name}) ${cxxReceiverClass!!.spelling}($parametersPart)"
                        }
                        isCxxDestructor ->
                            "(${bodyParameters[0].name})->~${cxxReceiverClass!!.spelling.substringAfterLast(':')}()"
                        else ->
                            if (context.configuration.library.language == Language.CPP)
                                "${fullName}(${bodyParameters.joinToString {"${it.type}(${it.name})"}})"
                            else
                                "${fullName}(${bodyParameters.joinToString {it.name}})"
                    }
                }

                val wrapperBody = if (function.returnType.unwrapTypedefs() is VoidType) {
                    "$callExpression;"
                } else {
                    "return (${returnType})$returnTypePrefix($callExpression)${returnTypePostfix};"
                }
                val wrapper = createWrapper(symbolName, wrapperName, returnType, signatureParameters, wrapperBody)
                CCalleeWrapper(wrapper)
            }

    fun generateCGlobalGetter(globalDecl: GlobalDecl, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${globalDecl.name}_getter")
        val returnType = globalDecl.type.getStringRepresentation()
        val wrapperBody = "return ${globalDecl.name};"
        val wrapper = createWrapper(symbolName, wrapperName, returnType, emptyList(), wrapperBody)
        return CCalleeWrapper(wrapper)
    }

    fun generateCGlobalByPointerGetter(globalDecl: GlobalDecl, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${globalDecl.name}_getter")
        val returnType = "void*"
        val wrapperBody = "return &${globalDecl.name};"
        val wrapper = createWrapper(symbolName, wrapperName, returnType, emptyList(), wrapperBody)
        return CCalleeWrapper(wrapper)
    }

    fun generateCGlobalSetter(globalDecl: GlobalDecl, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${globalDecl.name}_setter")
        val globalType = globalDecl.type.getStringRepresentation()
        val parameter = Parameter(globalType, "p1")
        val wrapperBody = "${globalDecl.name} = ${parameter.name};"
        val wrapper = createWrapper(symbolName, wrapperName, "void", listOf(parameter), wrapperBody)
        return CCalleeWrapper(wrapper)
    }
}
