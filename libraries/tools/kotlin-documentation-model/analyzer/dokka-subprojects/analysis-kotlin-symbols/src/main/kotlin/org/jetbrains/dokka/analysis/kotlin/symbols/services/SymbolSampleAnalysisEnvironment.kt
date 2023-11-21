/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleSnippet
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocTextLinkSymbol
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SamplesKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction

internal class SymbolSampleAnalysisEnvironmentCreator(
    private val context: DokkaContext,
) : SampleAnalysisEnvironmentCreator {

    private val projectKotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }

    override fun <T> use(block: SampleAnalysisEnvironment.() -> T): T {
        return runBlocking(Dispatchers.Default) {
            SamplesKotlinAnalysis(
                sourceSets = context.configuration.sourceSets,
                context = context
            ).use { samplesKotlinAnalysis ->
                val sampleAnalysisEnvironment = SymbolSampleAnalysisEnvironment(
                    samplesKotlinAnalysis = samplesKotlinAnalysis,
                    projectKotlinAnalysis = projectKotlinAnalysis,
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
    private val dokkaLogger: DokkaLogger,
) : SampleAnalysisEnvironment {

    override fun resolveSample(sourceSet: DokkaSourceSet, fullyQualifiedLink: String): SampleSnippet? {
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

        val imports = processImports(psiElement)
        val body = processBody(psiElement)

        return SampleSnippet(imports, body)
    }

    private fun findPsiElement(sourceSet: DokkaSourceSet, fqLink: String): PsiElement? {
        val ktSourceModule = samplesKotlinAnalysis.getModuleOrNull(sourceSet)
            ?: projectKotlinAnalysis.getModule(sourceSet)

        return analyze(ktSourceModule) {
            resolveKDocTextLinkSymbol(fqLink)
                ?.sourcePsiSafe()
        }
    }

    private fun processImports(psiElement: PsiElement): List<String> {
        val psiFile = psiElement.containingFile
        val importsList = (psiFile as? KtFile)?.importList ?: return emptyList()
        return importsList.imports
            .map { it.text.removePrefix("import ") }
            .filter { it.isNotBlank() }
    }

    private fun processBody(sampleElement: PsiElement): String {
        return getSampleBody(sampleElement)
            .trim { it == '\n' || it == '\r' }
            .trimEnd()
            .trimIndent()
    }

    private fun getSampleBody(sampleElement: PsiElement): String {
        return when (sampleElement) {
            is KtDeclarationWithBody -> {
                when (val bodyExpression = sampleElement.bodyExpression) {
                    is KtBlockExpression -> bodyExpression.text.removeSurrounding("{", "}")
                    else -> bodyExpression!!.text
                }
            }

            else -> sampleElement.text
        }
    }
}
