/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

object SymbolByFqName {
    fun getSymbolDataFromFile(filePath: String): SymbolData {
        val testFileText = FileUtil.loadFile(File(filePath))
        val identifier = testFileText.lineSequence().first { line -> SymbolData.identifiers.any { line.startsWith(it) } }
        return SymbolData.create(identifier)
    }

    fun textWithRenderedSymbolData(filePath: String, rendered: String): String = buildString {
        val testFileText = FileUtil.loadFile(File(filePath))
        val fileTextWithoutSymbolsData = testFileText.substringBeforeLast(SYMBOLS_TAG).trimEnd()
        appendLine(fileTextWithoutSymbolsData)
        appendLine()
        appendLine(SYMBOLS_TAG)
        append(rendered)
    }


    private const val SYMBOLS_TAG = "// SYMBOLS:"
}

sealed class SymbolData {
    abstract fun toSymbols(analysisSession: KtAnalysisSession): List<KtSymbol>

    data class ClassData(val classId: ClassId) : SymbolData() {
        override fun toSymbols(analysisSession: KtAnalysisSession): List<KtSymbol> {
            val symbol = analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(classId) ?: error("Class $classId is not found")
            return listOf(symbol)
        }
    }

    data class CallableData(val callableId: CallableId) : SymbolData() {
        override fun toSymbols(analysisSession: KtAnalysisSession): List<KtSymbol> {
            val classId = callableId.classId
            val symbols = if (classId == null) {
                analysisSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName).toList()
            } else {
                val classSymbol =
                    analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(classId)
                        ?: error("Class $classId is not found")
                analysisSession.scopeProvider.getDeclaredMemberScope(classSymbol).getCallableSymbols()
                    .filter { (it as? KtNamedSymbol)?.name == callableId.callableName }
                    .toList()
            }
            if (symbols.isEmpty()) {
                error("No callable with fqName $callableId found")
            }
            return symbols
        }
    }

    companion object {
        val identifiers = arrayOf("callable:", "class:")

        fun create(data: String): SymbolData = when {
            data.startsWith("class:") -> ClassData(ClassId.fromString(data.removePrefix("class:").trim()))
            data.startsWith("callable:") -> {
                val fullName = data.removePrefix("callable:").trim()
                val name = if ('.' in fullName) fullName.substringAfterLast(".") else fullName.substringAfterLast('/')
                val (packageName, className) = run {
                    val packageNameWithClassName = fullName.dropLast(name.length + 1)
                    when {
                        '.' in fullName ->
                            packageNameWithClassName.substringBeforeLast('/') to packageNameWithClassName.substringAfterLast('/')
                        else -> packageNameWithClassName to null
                    }
                }
                CallableData(CallableId(FqName(packageName.replace('/', '.')), className?.let { FqName(it) }, Name.identifier(name)))
            }
            else -> error("Invalid symbol")
        }
    }
}

