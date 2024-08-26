/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.base.generation.SourceSetIdUniquenessChecker
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.*
import org.jetbrains.dokka.base.renderers.html.command.consumers.PathToRootConsumer
import org.jetbrains.dokka.base.renderers.html.command.consumers.ReplaceVersionsConsumer
import org.jetbrains.dokka.base.renderers.html.command.consumers.ResolveLinkConsumer
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.*
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.base.transformers.pages.tags.CustomTagContentProvider
import org.jetbrains.dokka.base.transformers.pages.tags.SinceKotlinTagContentProvider
import org.jetbrains.dokka.base.translators.documentables.DefaultDocumentableToPageTranslator
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.documentation.*
import org.jetbrains.dokka.transformers.pages.PageTransformer

@Suppress("unused")
public class DokkaBase : DokkaPlugin() {

    public val preMergeDocumentableTransformer: ExtensionPoint<PreMergeDocumentableTransformer> by extensionPoint()
    public val pageMergerStrategy: ExtensionPoint<PageMergerStrategy> by extensionPoint()
    public val commentsToContentConverter: ExtensionPoint<CommentsToContentConverter> by extensionPoint()
    public val customTagContentProvider: ExtensionPoint<CustomTagContentProvider> by extensionPoint()
    public val signatureProvider: ExtensionPoint<SignatureProvider> by extensionPoint()
    public val locationProviderFactory: ExtensionPoint<LocationProviderFactory> by extensionPoint()
    public val externalLocationProviderFactory: ExtensionPoint<ExternalLocationProviderFactory> by extensionPoint()
    public val outputWriter: ExtensionPoint<OutputWriter> by extensionPoint()
    public val htmlPreprocessors: ExtensionPoint<PageTransformer> by extensionPoint()

    /**
     * Extension point for providing custom HTML code block renderers.
     *
     * This extension point allows overriding the rendering of code blocks in different programming languages.
     * Multiple renderers can be installed to support different languages independently.
     */
    public val htmlCodeBlockRenderers: ExtensionPoint<HtmlCodeBlockRenderer> by extensionPoint()

    @Deprecated("It is not used anymore")
    public val tabSortingStrategy: ExtensionPoint<TabSortingStrategy> by extensionPoint()
    public val immediateHtmlCommandConsumer: ExtensionPoint<ImmediateHtmlCommandConsumer> by extensionPoint()


    public val singleGeneration: Extension<Generation, *, *> by extending {
        CoreExtensions.generation providing ::SingleModuleGeneration
    }

    public val documentableMerger: Extension<DocumentableMerger, *, *> by extending {
        CoreExtensions.documentableMerger providing ::DefaultDocumentableMerger
    }

    public val deprecatedDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    public val suppressedDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::SuppressedByConfigurationDocumentableFilterTransformer
    }

    public val suppressedBySuppressTagDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::SuppressTagDocumentableFilter
    }

    public val jvmMappedMethodsFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::JvmMappedMethodsDocumentableFilterTransformer order {
            before(kotlinArrayDocumentableReplacer)
        }
    }

    public val documentableVisibilityFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    public val obviousFunctionsVisbilityFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::ObviousFunctionsDocumentableFilterTransformer
    }

    public val inheritedEntriesVisbilityFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::InheritedEntriesDocumentableFilterTransformer
    }

    public val kotlinArrayDocumentableReplacer: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::KotlinArrayDocumentableReplacerTransformer
    }

    public val emptyPackagesFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::EmptyPackagesFilterTransformer order {
            after(
                deprecatedDocumentableFilter,
                suppressedDocumentableFilter,
                documentableVisibilityFilter,
                suppressedBySuppressTagDocumentableFilter,
                obviousFunctionsVisbilityFilter,
                inheritedEntriesVisbilityFilter,
            )
        }
    }

    public val emptyModulesFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer with EmptyModulesFilterTransformer() order {
            after(emptyPackagesFilter)
        }
    }

    public val modulesAndPackagesDocumentation: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::ModuleAndPackageDocumentationTransformer
    }

    public val actualTypealiasAdder: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    public val kotlinSignatureProvider: Extension<SignatureProvider, *, *> by extending {
        signatureProvider providing ::KotlinSignatureProvider
    }

    public val sinceKotlinTransformer: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer providing ::SinceKotlinTransformer applyIf {
            DokkaBaseInternalConfiguration.sinceKotlinRenderingEnabled
        } order {
            before(extensionsExtractor)
        }
    }

    public val inheritorsExtractor: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with InheritorsExtractorTransformer()
    }

    public val undocumentedCodeReporter: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with ReportUndocumentedTransformer()
    }

    public val extensionsExtractor: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with ExtensionExtractorTransformer()
    }

    public val documentableToPageTranslator: Extension<DocumentableToPageTranslator, *, *> by extending {
        CoreExtensions.documentableToPageTranslator providing ::DefaultDocumentableToPageTranslator
    }

    public val docTagToContentConverter: Extension<CommentsToContentConverter, *, *> by extending {
        commentsToContentConverter with DocTagToContentConverter()
    }

    public val sinceKotlinTagContentProvider: Extension<CustomTagContentProvider, *, *> by extending {
        customTagContentProvider with SinceKotlinTagContentProvider applyIf {
            DokkaBaseInternalConfiguration.sinceKotlinRenderingEnabled
        }
    }

    public val pageMerger: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::PageMerger
    }

    public val sourceSetMerger: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::SourceSetMergingPageTransformer
    }

    public val fallbackMerger: Extension<PageMergerStrategy, *, *> by extending {
        pageMergerStrategy providing { ctx -> FallbackPageMergerStrategy(ctx.logger) }
    }

    public val sameMethodNameMerger: Extension<PageMergerStrategy, *, *> by extending {
        pageMergerStrategy providing { ctx -> SameMethodNamePageMergerStrategy(ctx.logger) } order {
            before(fallbackMerger)
        }
    }

    public val htmlRenderer: Extension<Renderer, *, *> by extending {
        CoreExtensions.renderer providing ::HtmlRenderer
    }

    public val locationProvider: Extension<LocationProviderFactory, *, *> by extending {
        locationProviderFactory providing ::DokkaLocationProviderFactory
    }

    public val javadocLocationProvider: Extension<ExternalLocationProviderFactory, *, *> by extending {
        externalLocationProviderFactory providing ::JavadocExternalLocationProviderFactory
    }

    public val dokkaLocationProvider: Extension<ExternalLocationProviderFactory, *, *> by extending {
        externalLocationProviderFactory providing ::DefaultExternalLocationProviderFactory
    }

    public val fileWriter: Extension<OutputWriter, *, *> by extending {
        outputWriter providing ::FileWriter
    }

    public val rootCreator: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors with RootCreator applyIf { !delayTemplateSubstitution }
    }

    public val defaultSamplesTransformer: Extension<PageTransformer, *, *> by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer order {
            before(pageMerger)
        }
    }

    public val sourceLinksTransformer: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing ::SourceLinksTransformer order { after(rootCreator) }
    }

    public val navigationPageInstaller: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing ::NavigationPageInstaller order { after(rootCreator) }
    }

    public val scriptsInstaller: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing ::ScriptsInstaller order { after(rootCreator) }
    }

    public val stylesInstaller: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing ::StylesInstaller order { after(rootCreator) }
    }

    public val assetsInstaller: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors with AssetsInstaller order { after(rootCreator) } applyIf { !delayTemplateSubstitution }
    }

    public val customResourceInstaller: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing { ctx -> CustomResourceInstaller(ctx) } order {
            after(stylesInstaller)
            after(scriptsInstaller)
            after(assetsInstaller)
        }
    }

    public val packageListCreator: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaHtml)
        } order { after(rootCreator) }
    }

    public val sourcesetDependencyAppender: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }

    public val resolveLinkConsumer: Extension<ImmediateHtmlCommandConsumer, *, *> by extending {
        immediateHtmlCommandConsumer with ResolveLinkConsumer
    }
    public val replaceVersionConsumer: Extension<ImmediateHtmlCommandConsumer, *, *> by extending {
        immediateHtmlCommandConsumer providing ::ReplaceVersionsConsumer
    }
    public val pathToRootConsumer: Extension<ImmediateHtmlCommandConsumer, *, *> by extending {
        immediateHtmlCommandConsumer with PathToRootConsumer
    }
    public val baseSearchbarDataInstaller: Extension<PageTransformer, *, *> by extending {
        htmlPreprocessors providing ::SearchbarDataInstaller order { after(sourceLinksTransformer) }
    }

    internal val sourceSetIdUniquenessChecker by extending {
        CoreExtensions.preGenerationCheck providing ::SourceSetIdUniquenessChecker
    }

    //<editor-fold desc="Deprecated API left for compatibility">
    @Suppress("DEPRECATION_ERROR")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val kotlinAnalysis: ExtensionPoint<org.jetbrains.dokka.analysis.KotlinAnalysis> by extensionPoint()

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val externalDocumentablesProvider: ExtensionPoint<org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider> by extensionPoint()

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val externalClasslikesTranslator: ExtensionPoint<org.jetbrains.dokka.base.translators.descriptors.ExternalClasslikesTranslator> by extensionPoint()

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val descriptorToDocumentableTranslator: org.jetbrains.dokka.plugability.Extension<org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator, *, *>
        get() = throw org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError()

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val psiToDocumentableTranslator: org.jetbrains.dokka.plugability.Extension<org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator, *, *>
        get() = throw org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError()

    @Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val defaultKotlinAnalysis: org.jetbrains.dokka.plugability.Extension<org.jetbrains.dokka.analysis.KotlinAnalysis, *, *>
        get() = throw org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError()

    @Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val defaultExternalDocumentablesProvider: org.jetbrains.dokka.plugability.Extension<org.jetbrains.dokka.base.translators.descriptors.ExternalDocumentablesProvider, *, *>
        get() = throw org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError()

    @Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")
    @Deprecated(message = org.jetbrains.dokka.base.deprecated.ANALYSIS_API_DEPRECATION_MESSAGE, level = DeprecationLevel.ERROR)
    public val defaultExternalClasslikesTranslator: org.jetbrains.dokka.plugability.Extension<org.jetbrains.dokka.base.translators.descriptors.ExternalClasslikesTranslator, *, *>
        get() = throw org.jetbrains.dokka.base.deprecated.AnalysisApiDeprecatedError()
    //</editor-fold>

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
