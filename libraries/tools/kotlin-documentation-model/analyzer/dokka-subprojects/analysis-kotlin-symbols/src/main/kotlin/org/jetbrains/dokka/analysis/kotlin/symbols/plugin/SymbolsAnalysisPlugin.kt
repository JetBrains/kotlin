/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.plugin

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.psi.PsiAnnotation
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java.KotlinInheritDocTagContentProvider
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java.DescriptorKotlinDocCommentCreator
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java.KotlinDocCommentParser
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.analysis.kotlin.symbols.services.KotlinAnalysisSourceRootsExtractor
import org.jetbrains.dokka.analysis.kotlin.symbols.services.*
import org.jetbrains.dokka.analysis.kotlin.symbols.services.KotlinDocumentableSourceLanguageParser
import org.jetbrains.dokka.analysis.kotlin.symbols.services.SymbolExternalDocumentablesProvider
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.DefaultSymbolToDocumentableTranslator
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation

@Suppress("unused")
public class SymbolsAnalysisPlugin : DokkaPlugin() {

    internal val kotlinAnalysis by extensionPoint<KotlinAnalysis>()

    internal val defaultKotlinAnalysis by extending {
        kotlinAnalysis providing { ctx ->
            ProjectKotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                context = ctx
            )
        }
    }


    internal val disposeKotlinAnalysisPostAction by extending {
        CoreExtensions.postActions providing { context ->
            PostAction {
                querySingle { kotlinAnalysis }.close()
                if (context.configuration.finalizeCoroutines) {
                    /**
                     * Disposes global resources which would persist after unloading Analysis API (Symbols analysis) and IJ platform classes.
                     *
                     * **Important:** Once this function has been called, Analysis API *and* IntelliJ platform classes should not be used anymore. The classes
                     * should either be unloaded or the whole program should be shut down.
                     *
                     * Note: Disposing of resources, including threads, allows unloading Dokka's class loader.
                     */
                    @OptIn(KaExperimentalApi::class)
                    org.jetbrains.kotlin.analysis.api.standalone.disposeGlobalStandaloneApplicationServices()
                }
            }
        }
    }

    internal val symbolToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultSymbolToDocumentableTranslator
    }

    private val javaAnalysisPlugin by lazy { plugin<JavaAnalysisPlugin>() }

    internal val projectProvider by extending {
        javaAnalysisPlugin.projectProvider providing { KotlinAnalysisProjectProvider() }
    }

    internal val sourceRootsExtractor by extending {
        javaAnalysisPlugin.sourceRootsExtractor providing { KotlinAnalysisSourceRootsExtractor() }
    }

    internal val kotlinDocCommentCreator by extending {
        javaAnalysisPlugin.docCommentCreators providing {
            DescriptorKotlinDocCommentCreator()
        }
    }

    internal val kotlinDocCommentParser by extending {
        javaAnalysisPlugin.docCommentParsers providing { context ->
            KotlinDocCommentParser(
                context
            )
        }
    }
    internal val inheritDocTagProvider by extending {
        javaAnalysisPlugin.inheritDocTagContentProviders providing ::KotlinInheritDocTagContentProvider
    }
    internal val kotlinLightMethodChecker by extending {
        javaAnalysisPlugin.kotlinLightMethodChecker providing {
            object : BreakingAbstractionKotlinLightMethodChecker {
                override fun isLightAnnotation(annotation: PsiAnnotation): Boolean {
                    return annotation is KtLightAbstractAnnotation
                }

                override fun isLightAnnotationAttribute(attribute: JvmAnnotationAttribute): Boolean {
                    return attribute is KtLightAbstractAnnotation
                }
            }
        }
    }


    internal val symbolAnalyzerImpl by extending {
        plugin<InternalKotlinAnalysisPlugin>().documentableSourceLanguageParser providing { KotlinDocumentableSourceLanguageParser() }
    }

    internal val symbolFullClassHierarchyBuilder by extending {
        plugin<InternalKotlinAnalysisPlugin>().fullClassHierarchyBuilder providing ::SymbolFullClassHierarchyBuilder
    }

    internal val symbolSyntheticDocumentableDetector by extending {
        plugin<InternalKotlinAnalysisPlugin>().syntheticDocumentableDetector providing { SymbolSyntheticDocumentableDetector() }
    }

    internal val moduleAndPackageDocumentationReader by extending {
        plugin<InternalKotlinAnalysisPlugin>().moduleAndPackageDocumentationReader providing ::ModuleAndPackageDocumentationReader
    }

    internal val kotlinToJavaMapper by extending {
         plugin<InternalKotlinAnalysisPlugin>().kotlinToJavaService providing { SymbolKotlinToJavaMapper() }
     }

    internal val symbolInheritanceBuilder by extending {
         plugin<InternalKotlinAnalysisPlugin>().inheritanceBuilder providing ::SymbolInheritanceBuilder
     }

    internal val symbolExternalDocumentableProvider by extending {
        plugin<KotlinAnalysisPlugin>().externalDocumentableProvider providing ::SymbolExternalDocumentablesProvider
    }

    internal val symbolSampleAnalysisEnvironmentCreator  by extending {
        plugin<KotlinAnalysisPlugin>().sampleAnalysisEnvironmentCreator providing ::SymbolSampleAnalysisEnvironmentCreator
    }

    internal val sourceRootIndependentChecker by extending {
        CoreExtensions.preGenerationCheck providing ::SourceRootIndependentChecker
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
