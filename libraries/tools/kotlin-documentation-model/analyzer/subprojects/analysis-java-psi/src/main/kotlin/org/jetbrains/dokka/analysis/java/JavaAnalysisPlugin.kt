/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
import java.io.File


@InternalDokkaApi
public interface ProjectProvider {
    public fun getProject(sourceSet: DokkaSourceSet, context: DokkaContext): Project
}

@InternalDokkaApi
public interface SourceRootsExtractor {
    public fun extract(sourceSet: DokkaSourceSet, context: DokkaContext): List<File>
}

@InternalDokkaApi
public interface BreakingAbstractionKotlinLightMethodChecker {
    // TODO [beresnev] not even sure it's needed, but left for compatibility and to preserve behaviour
    public fun isLightAnnotation(annotation: PsiAnnotation): Boolean
    public fun isLightAnnotationAttribute(attribute: JvmAnnotationAttribute): Boolean
}

@InternalDokkaApi
public class JavaAnalysisPlugin : DokkaPlugin() {

    // single
    public val projectProvider: ExtensionPoint<ProjectProvider> by extensionPoint()

    // single
    public val sourceRootsExtractor: ExtensionPoint<SourceRootsExtractor> by extensionPoint()

    // multiple
    public val docCommentCreators: ExtensionPoint<DocCommentCreator> by extensionPoint()

    // multiple
    public val docCommentParsers: ExtensionPoint<DocCommentParser> by extensionPoint()

    // none or more
    public val inheritDocTagContentProviders: ExtensionPoint<InheritDocTagContentProvider> by extensionPoint()

    // TODO [beresnev] figure out a better way depending on what it's used for
    public val kotlinLightMethodChecker: ExtensionPoint<BreakingAbstractionKotlinLightMethodChecker> by extensionPoint()

    private val docCommentFactory by lazy {
        DocCommentFactory(query { docCommentCreators }.reversed())
    }

    public val docCommentFinder: DocCommentFinder by lazy {
        DocCommentFinder(logger, docCommentFactory)
    }

    internal val javaDocCommentCreator by extending {
        docCommentCreators providing { JavaDocCommentCreator() }
    }

    private val psiDocTagParser by lazy {
        PsiDocTagParser(
            inheritDocTagResolver = InheritDocTagResolver(
                docCommentFactory = docCommentFactory,
                docCommentFinder = docCommentFinder,
                contentProviders = query { inheritDocTagContentProviders }
            )
        )
    }

    internal val javaDocCommentParser by extending {
        docCommentParsers providing {
            JavaPsiDocCommentParser(
                psiDocTagParser
            )
        }
    }

    internal val psiToDocumentableTranslator by extending {
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
