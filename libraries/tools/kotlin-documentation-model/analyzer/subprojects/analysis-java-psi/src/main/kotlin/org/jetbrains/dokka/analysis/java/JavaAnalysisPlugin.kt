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
interface ProjectProvider {
    fun getProject(sourceSet: DokkaSourceSet, context: DokkaContext): Project
}

@InternalDokkaApi
interface SourceRootsExtractor {
    fun extract(sourceSet: DokkaSourceSet, context: DokkaContext): List<File>
}

@InternalDokkaApi
interface BreakingAbstractionKotlinLightMethodChecker {
    // TODO [beresnev] not even sure it's needed, but left for compatibility and to preserve behaviour
    fun isLightAnnotation(annotation: PsiAnnotation): Boolean
    fun isLightAnnotationAttribute(attribute: JvmAnnotationAttribute): Boolean
}

@InternalDokkaApi
class JavaAnalysisPlugin : DokkaPlugin() {

    // single
    val projectProvider by extensionPoint<ProjectProvider>()

    // single
    val sourceRootsExtractor by extensionPoint<SourceRootsExtractor>()

    // multiple
    val docCommentCreators by extensionPoint<DocCommentCreator>()

    // multiple
    val docCommentParsers by extensionPoint<DocCommentParser>()

    // none or more
    val inheritDocTagContentProviders by extensionPoint<InheritDocTagContentProvider>()

    // TODO [beresnev] figure out a better way depending on what it's used for
    val kotlinLightMethodChecker by extensionPoint<BreakingAbstractionKotlinLightMethodChecker>()

    private val docCommentFactory by lazy {
        DocCommentFactory(query { docCommentCreators }.reversed())
    }

    val docCommentFinder by lazy {
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
