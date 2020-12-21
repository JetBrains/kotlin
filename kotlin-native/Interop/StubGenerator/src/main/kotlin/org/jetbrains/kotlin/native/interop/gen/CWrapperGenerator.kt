/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

internal data class CCalleeWrapper(val lines: List<String>)

open class ManagedTypePassing {
    open val ManagedType.passValue: String get() = error("ManagedType support requires a plugin")
    open val ManagedType.returnValue: String get() = error("ManagedType support requires a plugin")
}

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

    private fun bindSymbolToFunction(symbol: String, function: String): List<String> {
        val prefix = if (context.configuration.library.language == Language.CPP)
            "extern \"C\" "
        else
            ""

        return listOf(
                "${prefix}const void* $symbol __asm(${symbol.quoteAsKotlinLiteral()});",
                "${prefix}const void* $symbol = (const void*)&$function;"
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

    private val Type.stringRepresentation get() = this.getStringRepresentation(context.plugin)

    private fun createCCalleeWrapper(function: FunctionDecl, symbolName: String): List<String> {
        assert(context.configuration.library.language != Language.CPP)

        val wrapperName = generateFunctionWrapperName(function.name)

        val returnType = function.returnType.stringRepresentation

        val parameters = function.parameters.mapIndexed { index, parameter ->
            val type = parameter.type.stringRepresentation
            Parameter(type, "p$index")
        }

        val callExpression = "${function.name}(${parameters.joinToString { it.name }})"

        val wrapperBody = if (function.returnType.unwrapTypedefs() is VoidType) {
            "$callExpression;"
        } else {
            "return (${returnType})($callExpression);"
        }
        return createWrapper(symbolName, wrapperName, returnType, parameters, wrapperBody)
    }

    private fun createCppCalleeWrapper(function: FunctionDecl, symbolName: String): List<String> {
        assert(context.configuration.library.language == Language.CPP)
        
        val wrapperName = generateFunctionWrapperName(function.name)

        val returnType = function.returnType.stringRepresentation
        val unwrappedReturnType = function.returnType.unwrapTypedefs()
        val returnTypePrefix =
                if (unwrappedReturnType is PointerType && unwrappedReturnType.isLVReference) "&" else ""
        val returnTypePostfix =
                if (unwrappedReturnType is ManagedType)
                    with(context.plugin.managedTypePassing) { unwrappedReturnType.returnValue }
                else ""

        val parameters = function.parameters.mapIndexed { index, parameter ->
            val type = parameter.type.stringRepresentation
            Parameter(type, "p$index")
        }
        val argumentTypes = function.parameters.map { parameter ->
            val parameterTypeText = parameter.type.stringRepresentation
            val type = parameter.type
            val unwrappedType = type.unwrapTypedefs()
            
            val cppRefTypePrefix =
                        if (unwrappedType is PointerType && unwrappedType.isLVReference) "*" else ""
            val typeExpression = when {
                type is Typedef ->
                    "(${type.def.name})"
                type is PointerType && type.spelling != null ->
                    "(${type.spelling})$cppRefTypePrefix"
                unwrappedType is EnumType ->
                    "(${unwrappedType.def.spelling})"
                unwrappedType is RecordType ->
                    "*(${unwrappedType.decl.spelling}*)"
                unwrappedType is ManagedType -> {
                    with(context.plugin.managedTypePassing) { unwrappedType.passValue }
                }
                else ->
                    "$cppRefTypePrefix($parameterTypeText)"
            }

            typeExpression
        }

        val callExpression = with (function) {
            assert(argumentTypes.size == parameters.size)
            val arguments = argumentTypes.mapIndexed { index, type ->
                "${type}(${parameters[index].name})"
            }
            when {
                isCxxInstanceMethod -> {
                    val parametersPart = arguments.drop(1).joinToString()
                    "(${parameters[0].name})->${name}($parametersPart)"
                }
                isCxxConstructor -> {
                    val parametersPart = arguments.drop(1).joinToString()
                    "new(${parameters[0].name}) ${cxxReceiverClass!!.spelling}($parametersPart)"
                }
                isCxxDestructor ->
                    "(${parameters[0].name})->~${cxxReceiverClass!!.spelling.substringAfterLast(':')}()"
                else -> "${fullName}(${arguments.joinToString()})"
            }
        }

        val wrapperBody = if (function.returnType.unwrapTypedefs() is VoidType) {
            "$callExpression;"
        } else {
            "return (${returnType})$returnTypePrefix($callExpression)$returnTypePostfix;"
        }
        return createWrapper(symbolName, wrapperName, returnType, parameters, wrapperBody)
    }

    fun generateCCalleeWrapper(function: FunctionDecl, symbolName: String): CCalleeWrapper =
            if (function.isVararg) {
                CCalleeWrapper(bindSymbolToFunction(symbolName, function.name))
            } else {
                val wrapper = if (context.configuration.library.language == Language.CPP) {
                    createCppCalleeWrapper(function, symbolName)
                } else {
                    createCCalleeWrapper(function, symbolName)
                }
                CCalleeWrapper(wrapper)
            }

    fun generateCGlobalGetter(globalDecl: GlobalDecl, symbolName: String): CCalleeWrapper {
        val wrapperName = generateFunctionWrapperName("${globalDecl.name}_getter")
        val returnType = globalDecl.type.stringRepresentation
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
        val globalType = globalDecl.type.stringRepresentation
        val parameter = Parameter(globalType, "p1")
        val wrapperBody = "${globalDecl.name} = ${parameter.name};"
        val wrapper = createWrapper(symbolName, wrapperName, "void", listOf(parameter), wrapperBody)
        return CCalleeWrapper(wrapper)
    }
}
