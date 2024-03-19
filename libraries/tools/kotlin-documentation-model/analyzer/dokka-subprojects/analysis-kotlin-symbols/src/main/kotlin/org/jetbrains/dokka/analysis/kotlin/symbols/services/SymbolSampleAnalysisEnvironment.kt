/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleRewriter
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocTextLinkToSymbol
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal class SymbolSampleAnalysisEnvironmentCreator(
    private val context: DokkaContext,
) : SampleAnalysisEnvironmentCreator {

    private val projectKotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
    private val sampleRewriter by lazy {
        val rewriters = context.plugin<KotlinAnalysisPlugin>().query { sampleRewriter }
        if (rewriters.size > 1) context.logger.warn("There are more than one samples rewriters. Dokka does not support it.")
        rewriters.singleOrNull()
    }


    override fun <T> use(block: SampleAnalysisEnvironment.() -> T): T {
        return runBlocking(Dispatchers.Default) {
            SamplesKotlinAnalysis(
                sourceSets = context.configuration.sourceSets,
                context = context
            ).use { samplesKotlinAnalysis ->
                val sampleAnalysisEnvironment = SymbolSampleAnalysisEnvironment(
                    samplesKotlinAnalysis = samplesKotlinAnalysis,
                    projectKotlinAnalysis = projectKotlinAnalysis,
                    sampleRewriter = sampleRewriter,
                    dokkaLogger = context.logger
                )
                block(sampleAnalysisEnvironment)
            }
        }
    }
}

private class SymbolSampleAnalysisEnvironment(
    private val samplesKotlinAnalysis: KotlinAnalysis,
    private val projectKotlinAnalysis: KotlinAnalysis,
    private val sampleRewriter: SampleRewriter?,
    private val dokkaLogger: DokkaLogger,
) : SampleAnalysisEnvironment {

    override fun resolveSample(
        sourceSet: DokkaSourceSet,
        fullyQualifiedLink: String
    ): SampleSnippet? {
        val psiElement = findPsiElement(sourceSet, fullyQualifiedLink)
        if (psiElement == null) {
            dokkaLogger.warn(
                "Unable to resolve a @sample link: \"$fullyQualifiedLink\". Is it used correctly? " +
                        "Expecting a link to a reachable (resolvable) top-level Kotlin function."
            )
            return null
        } else if (psiElement.language != KotlinLanguage.INSTANCE) {
            dokkaLogger.warn("Unable to resolve non-Kotlin @sample links: \"$fullyQualifiedLink\"")
            return null
        } else if (psiElement !is KtFunction) {
            dokkaLogger.warn("Unable to process a @sample link: \"$fullyQualifiedLink\". Only function links allowed.")
            return null
        }

        val imports = processImports(psiElement, sampleRewriter)
        val body = processBody(psiElement)

        return SampleSnippet(imports, body)
    }

    // TODO: remove after KT-53669 and use [org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe] from Analysis API
    private inline fun <reified PSI : PsiElement> KtSymbol.kotlinAndJavaSourcePsiSafe(): PSI? {
        // TODO: support Java sources after KT-53669
        val sourcePsi = when (origin) {
            KtSymbolOrigin.SOURCE -> this.psi
            KtSymbolOrigin.JAVA -> this.psi

            KtSymbolOrigin.SOURCE_MEMBER_GENERATED -> null
            KtSymbolOrigin.LIBRARY -> null
            KtSymbolOrigin.SAM_CONSTRUCTOR -> null
            KtSymbolOrigin.INTERSECTION_OVERRIDE -> null
            KtSymbolOrigin.SUBSTITUTION_OVERRIDE -> null
            KtSymbolOrigin.DELEGATED -> null
            KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY -> null
            KtSymbolOrigin.PROPERTY_BACKING_FIELD -> null
            KtSymbolOrigin.PLUGIN -> null
            KtSymbolOrigin.JS_DYNAMIC -> null
        }

        return sourcePsi as? PSI
    }

    private fun findPsiElement(sourceSet: DokkaSourceSet, fqLink: String): PsiElement? {
        // fallback to default roots of the source set even if sample roots are assigned,
        // because `@sample` tag can contain links to functions from project sources
        return samplesKotlinAnalysis.findPsiElement(sourceSet, fqLink)
            ?: projectKotlinAnalysis.findPsiElement(sourceSet, fqLink)
    }

    private fun KotlinAnalysis.findPsiElement(sourceSet: DokkaSourceSet, fqLink: String): PsiElement? {
        val ktSourceModule = this.getModuleOrNull(sourceSet) ?: return null
        return analyze(ktSourceModule) {
            resolveKDocTextLinkToSymbol(fqLink)
                ?.kotlinAndJavaSourcePsiSafe()
        }
    }

    private fun processImports(psiElement: PsiElement, sampleRewriter: SampleRewriter?): List<String> {
        val psiFile = psiElement.containingFile
        val importsList = (psiFile as? KtFile)?.importList ?: return emptyList()
        return importsList.imports
            .map { it.text.removePrefix("import ") }
            .filter { it.isNotBlank() }
            .applyIf(sampleRewriter != null) {
                mapNotNull { sampleRewriter?.rewriteImportDirective(it) }
            }
    }

    private fun processBody(sampleElement: KtDeclarationWithBody): String {
        return getSampleBody(sampleElement)
            .trim { it == '\n' || it == '\r' }
            .trimEnd()
            .trimIndent()
    }

    private fun getSampleBody(psiElement: KtDeclarationWithBody): String {
        val bodyExpression = psiElement.bodyExpression
        val bodyExpressionText = bodyExpression!!.buildSampleText()
        return when (bodyExpression) {
            is KtBlockExpression -> bodyExpressionText.removeSurrounding("{", "}") // without braces according to the documentation of [SampleSnippet.body]
            else -> bodyExpressionText
        }
    }

    private fun PsiElement.buildSampleText(): String {
        if (sampleRewriter == null) return this.text

        val textBuilder = StringBuilder()
        val errors = mutableListOf<SampleBuilder.ConvertError>()

        this.accept(SampleBuilder(sampleRewriter, textBuilder, errors))

        errors.forEach {
            val st = it.e.stackTraceToString()

            dokkaLogger.warn("Exception thrown while sample rewriting at ${containingFile.name}: (${it.loc})\n```\n${it.text}\n```\n$st")
        }
        return textBuilder.toString()
    }
}

private class SampleBuilder(
    private val sampleRewriter: SampleRewriter,
    val textBuilder: StringBuilder,
    val errors: MutableList<ConvertError>
) : KtTreeVisitorVoid() {

    data class ConvertError(val e: Exception, val text: String, val loc: String)

    override fun visitCallExpression(expression: KtCallExpression) {
        val callRewriter = expression.calleeExpression?.text?.let { sampleRewriter.getFunctionCallRewriter(it) }
        if(callRewriter != null) {
            val rewrittenResult = callRewriter.rewrite(
                arguments = expression.valueArguments.map { it.text ?: "" }, // expect not nullable ASTDelegatePsiElement.text
                typeArguments = expression.typeArguments.map { it.text ?: "" } // expect not nullable ASTDelegatePsiElement.text
            )

            textBuilder.append(rewrittenResult)
        } else {
            super.visitCallExpression(expression)
        }
    }

    private fun reportProblemConvertingElement(element: PsiElement, e: Exception) {
        val text = element.text
        val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile)

        val lineInfo = if (document != null) {
            val lineNumber = document.getLineNumber(element.startOffset)
            "$lineNumber, ${element.startOffset - document.getLineStartOffset(lineNumber)}"
        } else {
            "offset: ${element.startOffset}"
        }
        errors += ConvertError(e, text, lineInfo)
    }

    override fun visitElement(element: PsiElement) {
        if (element is LeafPsiElement) {
            textBuilder.append(element.text)
            return
        }

        element.acceptChildren(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                try {
                    element.accept(this@SampleBuilder)
                } catch (e: Exception) {
                    try {
                        reportProblemConvertingElement(element, e)
                    } finally {
                        textBuilder.append(element.text) //recover
                    }
                }
            }
        })
    }
}
