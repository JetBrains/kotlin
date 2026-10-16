/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentCreator
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentFactory
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentFinder
import org.jetbrains.dokka.analysis.java.doccomment.JavaDocCommentCreator
import org.jetbrains.dokka.analysis.java.parsers.DocCommentParser
import org.jetbrains.dokka.analysis.java.parsers.doctag.InheritDocTagContentProvider
import org.jetbrains.dokka.analysis.java.parsers.JavaPsiDocCommentParser
import org.jetbrains.dokka.analysis.java.parsers.doctag.InheritDocTagResolver
import org.jetbrains.dokka.analysis.java.parsers.doctag.PsiDocTagParser
import org.jetbrains.dokka.analysis.java.util.NoopIntellijLoggerFactory
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import java.io.File


@InternalDokkaApi
internal interface ProjectProvider {
    fun getProject(sourceSet: DokkaSourceSet, context: DokkaContext): Project
}

@InternalDokkaApi
internal interface SourceRootsExtractor {
    fun extract(sourceSet: DokkaSourceSet, context: DokkaContext): List<File>
}

@InternalDokkaApi
internal interface BreakingAbstractionKotlinLightMethodChecker {
    // TODO [beresnev] not even sure it's needed, but left for compatibility and to preserve behaviour
    fun isLightAnnotation(annotation: PsiAnnotation): Boolean
    fun isLightAnnotationAttribute(attribute: JvmAnnotationAttribute): Boolean
}

@InternalDokkaApi
internal class JavaAnalysisPlugin : DokkaPlugin() {

    // single
    val projectProvider: ExtensionPoint<ProjectProvider> by extensionPoint()

    // single
    val sourceRootsExtractor: ExtensionPoint<SourceRootsExtractor> by extensionPoint()

    // multiple
    val docCommentCreators: ExtensionPoint<DocCommentCreator> by extensionPoint()

    // multiple
    val docCommentParsers: ExtensionPoint<DocCommentParser> by extensionPoint()

    // none or more
    val inheritDocTagContentProviders: ExtensionPoint<InheritDocTagContentProvider> by extensionPoint()

    // TODO [beresnev] figure out a better way depending on what it's used for
    val kotlinLightMethodChecker: ExtensionPoint<BreakingAbstractionKotlinLightMethodChecker> by extensionPoint()

    private val docCommentFactory by lazy {
        DocCommentFactory(query { docCommentCreators }.reversed())
    }

    val docCommentFinder: DocCommentFinder by lazy {
        DocCommentFinder(logger, docCommentFactory)
    }

    internal val javaDocCommentCreator by extending {
        docCommentCreators providing { JavaDocCommentCreator() }
    }

    internal val javaDocCommentParser by extending {
        docCommentParsers providing { ctx ->
            JavaPsiDocCommentParser(
                PsiDocTagParser(
                    context = ctx,
                    inheritDocTagResolver = InheritDocTagResolver(
                        docCommentFactory = docCommentFactory,
                        docCommentFinder = docCommentFinder,
                        contentProviders = query { inheritDocTagContentProviders }
                    )
                )
            )
        }
    }

    @InternalDokkaApi
    val psiToDocumentableTranslator: Extension<SourceToDocumentableTranslator, *, *> by extending {
        CoreExtensions.sourceToDocumentableTranslator providing { DefaultPsiToDocumentableTranslator() }
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement

    private companion object {
        init {
            // Suppress messages emitted by the IntelliJ logger since
            // there's not much the end user can do about it
            Logger.setFactory(NoopIntellijLoggerFactory())
        }
    }
}
