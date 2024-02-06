/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifierListOwner
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.java.parsers.DokkaPsiParser
import org.jetbrains.dokka.analysis.java.parsers.JavaPsiDocCommentParser
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.JavaVisibility
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.sources.AsyncSourceToDocumentableTranslator
import org.jetbrains.dokka.utilities.parallelMap
import org.jetbrains.dokka.utilities.parallelMapNotNull

internal class DefaultPsiToDocumentableTranslator : AsyncSourceToDocumentableTranslator {

    override suspend fun invokeSuspending(sourceSet: DokkaSourceSet, context: DokkaContext): DModule {
        return coroutineScope {
            val projectProvider = context.plugin<JavaAnalysisPlugin>().querySingle { projectProvider }
            val project = projectProvider.getProject(sourceSet, context)

            val sourceRootsExtractor = context.plugin<JavaAnalysisPlugin>().querySingle { sourceRootsExtractor }
            val sourceRoots = sourceRootsExtractor.extract(sourceSet, context)

            val localFileSystem = VirtualFileManager.getInstance().getFileSystem("file")

            val psiFiles = sourceRoots.parallelMap { sourceRoot ->
                sourceRoot.absoluteFile.walkTopDown().mapNotNull {
                    localFileSystem.findFileByPath(it.path)?.let { vFile ->
                        PsiManager.getInstance(project).findFile(vFile) as? PsiJavaFile
                    }
                }.toList()
            }.flatten()

            val docParser = createPsiParser(sourceSet, context)

            DModule(
                name = context.configuration.moduleName,
                packages = psiFiles.groupBy { it.packageName }.toList()
                    .parallelMap { (packageName: String, psiFiles: List<PsiJavaFile>) ->
                        docParser.parsePackage(packageName, psiFiles)
                    },
                documentation = emptyMap(),
                expectPresentInSet = null,
                sourceSets = setOf(sourceSet)
            )
        }
    }

    private fun createPsiParser(sourceSet: DokkaSourceSet, context: DokkaContext): DokkaPsiParser {
        val projectProvider = context.plugin<JavaAnalysisPlugin>().querySingle { projectProvider }
        val docCommentParsers = context.plugin<JavaAnalysisPlugin>().query { docCommentParsers }
        return DokkaPsiParser(
            sourceSetData = sourceSet,
            project = projectProvider.getProject(sourceSet, context),
            logger = context.logger,
            javadocParser = JavadocParser(
                docCommentParsers = docCommentParsers,
                docCommentFinder = context.plugin<JavaAnalysisPlugin>().docCommentFinder
            ),
            javaPsiDocCommentParser = docCommentParsers.single { it is JavaPsiDocCommentParser } as JavaPsiDocCommentParser,
            lightMethodChecker = context.plugin<JavaAnalysisPlugin>().querySingle { kotlinLightMethodChecker }
        )
    }
}

internal fun PsiModifierListOwner.getVisibility() = modifierList?.let {
    val ml = it.children.toList()
    when {
        ml.any { it.text == PsiKeyword.PUBLIC } || it.hasModifierProperty("public") -> JavaVisibility.Public
        ml.any { it.text == PsiKeyword.PROTECTED } || it.hasModifierProperty("protected") -> JavaVisibility.Protected
        ml.any { it.text == PsiKeyword.PRIVATE } || it.hasModifierProperty("private") -> JavaVisibility.Private
        else -> JavaVisibility.Default
    }
} ?: JavaVisibility.Default
