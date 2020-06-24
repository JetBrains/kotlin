/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.plugins.relaxNG.compact.psi.util.PsiFunction
import org.jetbrains.kotlin.idea.frontend.api.CallInfo
import org.jetbrains.kotlin.idea.frontend.api.TypeInfo
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter


abstract class AbstractResolveCallTest : @Suppress("DEPRECATION") KotlinLightCodeInsightTestCase() {
    override fun getTestDataPath(): String = KotlinTestUtils.getHomeDirectory() + "/"

    protected fun doTest(path: String) {
        File(path).getExternalFiles().forEach(::addFile)
        configureByFile(path)
        val elements = editor.caretModel.caretsAndSelections.map { selection ->
            getSingleSelectedElement(selection)
        }

        val actualText = executeOnPooledThreadInReadAction {
            val analysisSession = FirAnalysisSession(file as KtFile)
            val callInfos = elements.map { element ->
                when (element) {
                    is KtCallExpression -> analysisSession.resolveCall(element)
                    is KtBinaryExpression -> analysisSession.resolveCall(element)
                    else -> error("Selected should be either KtCallExpression or KtBinaryExpression but was $element")
                }
            }

            if (callInfos.isEmpty()) {
                error("There are should be at least one call selected")
            }

            val textWithoutLatestComments = run {
                val rawText = File(path).readText()
                """(?m)^// CALL:\s.*$""".toRegex().replace(rawText, "").trimEnd()
            }
            buildString {
                append(textWithoutLatestComments)
                append("\n\n")
                callInfos.joinTo(this, separator = "\n") { info ->
                    "// CALL: ${info?.stringRepresentation()}"
                }
            }
        }
        KotlinTestUtils.assertEqualsToFile(File(path), actualText)
    }


    private fun getSingleSelectedElement(selection: CaretState): PsiElement {
        val selectionRange = selection.getTextRange()
        val elements = file.elementsInRange(selectionRange)
        if (elements.size != 1) {
            val selectionText = file.text.substring(selectionRange.startOffset, selectionRange.endOffset)
            error("Single element should be found for selection `$selectionText`, but $elements were found")
        }
        return elements.first()
    }

    private fun CaretState.getTextRange() = TextRange.create(
        editor.logicalPositionToOffset(selectionStart!!),
        editor.logicalPositionToOffset(selectionEnd!!)
    )

    private fun addFile(file: File) {
        addFile(FileUtil.loadFile(file, /* convertLineSeparators = */true), file.name)
    }

    private fun addFile(text: String, fileName: String) {
        runWriteAction {
            val virtualDir = LightPlatformTestCase.getSourceRoot()!!
            val virtualFile = virtualDir.createChildData(null, fileName)
            virtualFile.getOutputStream(null)!!.writer().use { it.write(text) }
        }
    }
}

private fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

private fun File.getExternalFiles(): List<File> {
    val directory = parentFile
    val externalFileName = "${nameWithoutExtension}.external"
    return directory.listFiles { _, name ->
        name == "$externalFileName.kt" || name == "$externalFileName.java"
    }!!.filterNotNull()
}

private fun CallInfo.stringRepresentation(): String {
    fun TypeInfo.render() = asDenotableTypeStringRepresentation().replace('/', '.')
    fun Any.stringValue(): String? = when (this) {
        is KtFunctionLikeSymbol -> buildString {
            append(if (this@stringValue is KtFunctionSymbol) fqName else "<constructor>")
            append("(")
            (this@stringValue as? KtFunctionSymbol)?.receiverType?.let { receiver ->
                append("<receiver>: ${receiver.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }
            valueParameters.joinTo(this) { parameter ->
                "${parameter.name}: ${parameter.type.render()}"
            }
            append(")")
            append(": ${type.render()}")
        }
        is KtParameterSymbol -> "$name: ${type.render()}"
        is Boolean -> toString()
        else -> error("unexpected parameter type ${this::class}")
    }

    val callInfoClass = this::class
    return buildString {
        append(callInfoClass.simpleName!!)
        append(": ")
        val propertyByName = callInfoClass.memberProperties.associateBy(KProperty1<*, *>::name)
        callInfoClass.primaryConstructor!!.parameters.joinTo(this) { parameter ->
            val value = propertyByName[parameter.name]!!.javaGetter!!(this@stringRepresentation)?.stringValue()
            "${parameter.name!!} = $value"
        }
    }
}

