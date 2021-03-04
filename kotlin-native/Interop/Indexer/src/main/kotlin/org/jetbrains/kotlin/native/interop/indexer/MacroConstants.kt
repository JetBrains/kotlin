/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*
import java.io.File

/**
 * Finds all "macro constants" and registers them as [NativeIndex.constants] in given index.
 */
internal fun findMacros(
        nativeIndex: NativeIndexImpl,
        compilation: CompilationWithPCH,
        translationUnit: CXTranslationUnit,
        headers: Set<CXFile?>
) {
    val names = collectMacroNames(nativeIndex, translationUnit, headers)
    // TODO: apply user-defined filters.
    val macros = expandMacros(compilation, names, typeConverter = { nativeIndex.convertType(it) })

    macros.filterIsInstanceTo(nativeIndex.macroConstants)
    macros.filterIsInstanceTo(nativeIndex.wrappedMacros)
}

private typealias TypeConverter = (CValue<CXType>) -> Type

/**
 * For each name expands the macro with this name declared in the library,
 * checking if it gets expanded to a constant expression.
 *
 * Note: in the worst case this method parses the code against the library a lot of times,
 * so it requires library headers precompiled to significantly speed up the parsing and avoid visiting headers' AST.
 *
 * @return the list of constants.
 */
private fun expandMacros(
        library: CompilationWithPCH,
        names: List<String>,
        typeConverter: TypeConverter
): List<MacroDef> {
    withIndex(excludeDeclarationsFromPCH = true) { index ->
        val sourceFile = library.createTempSource()
        val compilerArgs = library.compilerArgs.toMutableList()
        // We disable implicit function declaration to filter out cases when a macro is expanded as a function
        // or function-like construction (e.g. #define FOO throw()) but such a function is undeclared.
        compilerArgs += "-Werror=implicit-function-declaration"

        // Ensure libclang reports all errors:
        compilerArgs += "-ferror-limit=0"

        val translationUnit = parseTranslationUnit(index, sourceFile, compilerArgs, options = 0)
        try {
            val nameToMacroDef = mutableMapOf<String, MacroDef>()
            val unprocessedMacros = names.toMutableList()

            // Note: will be slow for a library with a lot of macros having unbalanced '{'. TODO: Optimize this case too.

            while (unprocessedMacros.isNotEmpty()) {
                val processedMacros =
                        tryExpandMacros(library, translationUnit, sourceFile, unprocessedMacros, typeConverter)

                unprocessedMacros -= (processedMacros.keys + unprocessedMacros.first())
                // Note: removing first macro should not have any effect, doing this to ensure the loop is finite.

                processedMacros.forEach { (name, macroDef) ->
                    if (macroDef != null) nameToMacroDef[name] = macroDef
                }
            }

            return names.mapNotNull { nameToMacroDef[it] }
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    }
}

/**
 * Tries to expand macros [names] defined in [library].
 * Returns the map of successfully processed macros with resulting constant as a value
 * or `null` if the result is not a constant (expression).
 *
 * As a side effect, modifies the [sourceFile] and reparses the [translationUnit].
 */
private fun tryExpandMacros(
        library: CompilationWithPCH,
        translationUnit: CXTranslationUnit,
        sourceFile: File,
        names: List<String>,
        typeConverter: TypeConverter
): Map<String, MacroDef?> {

    reparseWithCodeSnippets(library, translationUnit, sourceFile, names)

    val macrosWithErrorsInSnippetFunctionHeader = mutableSetOf<String>()
    val macrosWithErrorsInSnippetFunctionBody = mutableSetOf<String>()

    val preambleSize = library.preambleLines.size

    translationUnit.getErrorLineNumbers().map { it - preambleSize - 1 }.forEach { lineNumber ->
        val index = lineNumber / CODE_SNIPPET_LINES_NUMBER
        if (index >= 0 && index < names.size) {
            when (lineNumber % CODE_SNIPPET_LINES_NUMBER) {
                0 -> macrosWithErrorsInSnippetFunctionHeader += names[index]
                1 -> macrosWithErrorsInSnippetFunctionBody += names[index]
                else -> {}
            }
        }
    }

    val result = mutableMapOf<String, MacroDef?>()

    visitChildren(translationUnit) { cursor, _ ->
        if (cursor.kind == CXCursorKind.CXCursor_FunctionDecl) {
            val functionName = getCursorSpelling(cursor)
            if (functionName.startsWith(CODE_SNIPPET_FUNCTION_NAME_PREFIX)) {
                val macroName = functionName.removePrefix(CODE_SNIPPET_FUNCTION_NAME_PREFIX)
                if (macroName in macrosWithErrorsInSnippetFunctionHeader) {
                    // Code snippet is likely affected by previous macros' snippets, skip it for now.
                } else {
                    result[macroName] = if (macroName in macrosWithErrorsInSnippetFunctionBody) {
                        // Code snippet is likely unaffected by previous ones but parsed with its own errors,
                        // so suppose macro is processed successfully as non-expression:
                        null
                    } else {
                        processCodeSnippet(cursor, macroName, typeConverter)
                    }
                }
            }
        }
        CXChildVisitResult.CXChildVisit_Continue
    }

    return result
}

private const val CODE_SNIPPET_LINES_NUMBER = 3
private const val CODE_SNIPPET_FUNCTION_NAME_PREFIX = "kni_indexer_function_"

/**
 * Adds code snippets to be then processed with [processCodeSnippet] to the [sourceFile]
 * and reparses the [translationUnit].
 *
 *  - If a code snippet allows extracting the constant value using libclang API, we'll add a [ConstantDef] in the
 * native index and generate a Kotlin constant for it.
 *  - If the expression type can be inferred by libclang, we'll add a [WrappedMacroDef] in the native index and
 * generate a bridge for this macro.
 *  - Otherwise the macro is skipped.
 */
private fun reparseWithCodeSnippets(library: CompilationWithPCH,
                                    translationUnit: CXTranslationUnit, sourceFile: File,
                                    names: List<String>) {

    // TODO: consider using CXUnsavedFile instead of writing the modified file to OS file system.
    sourceFile.bufferedWriter().use { writer ->
        writer.appendPreamble(library)

        names.forEach { name ->
            val codeSnippetLines = when (library.language) {
                Language.C, Language.OBJECTIVE_C ->
                    listOf("void $CODE_SNIPPET_FUNCTION_NAME_PREFIX$name() {",
                            "    __auto_type KNI_INDEXER_VARIABLE_$name = $name;",
                            "}")
            }

            assert(codeSnippetLines.size == CODE_SNIPPET_LINES_NUMBER)
            codeSnippetLines.forEach { writer.appendLine(it) }
        }
    }
    clang_reparseTranslationUnit(translationUnit, 0, null, 0)
}

/**
 * Checks that [functionCursor] is parsed exactly as expected for the code appended by [reparseWithCodeSnippets],
 * and returns the constant on success.
 */
private fun processCodeSnippet(
        functionCursor: CValue<CXCursor>,
        name: String,
        typeConverter: TypeConverter
): MacroDef? {

    val kindsToSkip = setOf(CXCursorKind.CXCursor_CompoundStmt)
    var state = VisitorState.EXPECT_NODES_TO_SKIP
    var evalResultOrNull: CXEvalResult? = null
    var typeOrNull: Type? = null

    val visitor: CursorVisitor = { cursor, _ ->
        val kind = cursor.kind
        when {
            state == VisitorState.EXPECT_VARIABLE && kind == CXCursorKind.CXCursor_VarDecl -> {
                evalResultOrNull = clang_Cursor_Evaluate(cursor)
                state = VisitorState.EXPECT_VARIABLE_VALUE
                CXChildVisitResult.CXChildVisit_Recurse
            }

            state == VisitorState.EXPECT_VARIABLE_VALUE && clang_isExpression(kind) != 0 -> {
                typeOrNull = typeConverter(clang_getCursorType(cursor))
                state = VisitorState.EXPECT_END
                CXChildVisitResult.CXChildVisit_Continue
            }

            // Skip auxiliary elements.
            state == VisitorState.EXPECT_NODES_TO_SKIP && kind in kindsToSkip ->
                CXChildVisitResult.CXChildVisit_Recurse

            state == VisitorState.EXPECT_NODES_TO_SKIP && kind == CXCursorKind.CXCursor_DeclStmt -> {
                state = VisitorState.EXPECT_VARIABLE
                CXChildVisitResult.CXChildVisit_Recurse
            }

            else -> {
                state = VisitorState.INVALID
                CXChildVisitResult.CXChildVisit_Break
            }
        }
    }

    try {
        visitChildren(functionCursor, visitor)

        if (state != VisitorState.EXPECT_END) {
            return null
        }

        val type = typeOrNull!!
        return if (evalResultOrNull == null) {
            // The macro cannot be evaluated as a constant so we will wrap it in a bridge.
            when(type.unwrapTypedefs()) {
                is PrimitiveType,
                is PointerType,
                is ObjCPointer -> WrappedMacroDef(name, type)
                else -> null
            }
        } else {
            // Otherwise we can evaluate the expression and create a Kotlin constant for it.
            val evalResult = evalResultOrNull!!
            val evalResultKind = clang_EvalResult_getKind(evalResult)
            when (evalResultKind) {
                CXEvalResultKind.CXEval_Int ->
                    IntegerConstantDef(name, type, clang_EvalResult_getAsLongLong(evalResult))

                CXEvalResultKind.CXEval_Float ->
                    FloatingConstantDef(name, type, clang_EvalResult_getAsDouble(evalResult))

                CXEvalResultKind.CXEval_CFStr,
                CXEvalResultKind.CXEval_ObjCStrLiteral,
                CXEvalResultKind.CXEval_StrLiteral ->
                    if (evalResultKind == CXEvalResultKind.CXEval_StrLiteral && !type.canonicalIsPointerToChar()) {
                        // libclang doesn't seem to support wide string literals properly in this API;
                        // thus disable wide literals here:
                        null
                    } else {
                        StringConstantDef(name, type, clang_EvalResult_getAsStr(evalResult)!!.toKString())
                    }

                CXEvalResultKind.CXEval_Other,
                CXEvalResultKind.CXEval_UnExposed -> null
            }
        }

    } finally {
        evalResultOrNull?.let { clang_EvalResult_dispose(it) }
    }
}

enum class VisitorState {
    EXPECT_NODES_TO_SKIP,
    EXPECT_VARIABLE, EXPECT_VARIABLE_VALUE,
    EXPECT_END, INVALID
}

private fun collectMacroNames(nativeIndex: NativeIndexImpl, translationUnit: CXTranslationUnit, headers: Set<CXFile?>): List<String> {
    val result = mutableSetOf<String>()

    visitChildren(translationUnit) { cursor, _ ->
        val file = memScoped {
            val fileVar = alloc<CXFileVar>()
            clang_getFileLocation(clang_getCursorLocation(cursor), fileVar.ptr, null, null, null)
            fileVar.value
        }

        if (cursor.kind == CXCursorKind.CXCursor_MacroDefinition &&
                nativeIndex.library.includesDeclaration(cursor) &&
                file != null && // Builtin macros mostly seem to be useless.
                file in headers &&
                canMacroBeConstant(cursor))
        {
            val spelling = getCursorSpelling(cursor)
            result.add(spelling)
        }
        CXChildVisitResult.CXChildVisit_Continue
    }

    return result.toList()
}

private fun canMacroBeConstant(cursor: CValue<CXCursor>): Boolean {

    if (clang_Cursor_isMacroFunctionLike(cursor) != 0) {
        return false
    }

    // TODO: check number of tokens and filter out empty definitions;
    // Requires updating to 3.9.1 due to https://bugs.llvm.org//show_bug.cgi?id=9069

    return true
}