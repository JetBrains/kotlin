@file:Suppress("unused")

package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.*
import org.jetbrains.dokka.base.renderers.html.command.consumers.PathToRootConsumer
import org.jetbrains.dokka.base.renderers.html.command.consumers.ResolveLinkConsumer
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.templating.ImmediateHtmlCommandConsumer
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.*
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.base.translators.descriptors.DefaultDescriptorToDocumentableTranslator
import org.jetbrains.dokka.base.translators.documentables.DefaultDocumentableToPageTranslator
import org.jetbrains.dokka.base.translators.psi.DefaultPsiToDocumentableTranslator
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DokkaBase : DokkaPlugin() {

    val preMergeDocumentableTransformer by extensionPoint<PreMergeDocumentableTransformer>()
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val commentsToContentConverter by extensionPoint<CommentsToContentConverter>()
    val signatureProvider by extensionPoint<SignatureProvider>()
    val locationProviderFactory by extensionPoint<LocationProviderFactory>()
    val externalLocationProviderFactory by extensionPoint<ExternalLocationProviderFactory>()
    val outputWriter by extensionPoint<OutputWriter>()
    val htmlPreprocessors by extensionPoint<PageTransformer>()
    val kotlinAnalysis by extensionPoint<KotlinAnalysis>()
    val tabSortingStrategy by extensionPoint<TabSortingStrategy>()
    val immediateHtmlCommandConsumer by extensionPoint<ImmediateHtmlCommandConsumer>()

    val singleGeneration by extending {
        CoreExtensions.generation providing ::SingleModuleGeneration
    }

    val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultDescriptorToDocumentableTranslator
    }

    val psiToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultPsiToDocumentableTranslator
    }

    val documentableMerger by extending {
        CoreExtensions.documentableMerger providing ::DefaultDocumentableMerger
    }

    val deprecatedDocumentableFilter by extending {
        preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    val suppressedDocumentableFilter by extending {
        preMergeDocumentableTransformer providing ::SuppressedByConfigurationDocumentableFilterTransformer
    }

    val suppressedBySuppressTagDocumentableFilter by extending {
        preMergeDocumentableTransformer providing ::SuppressTagDocumentableFilter
    }

    val documentableVisibilityFilter by extending {
        preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    val obviousFunctionsVisbilityFilter by extending {
        preMergeDocumentableTransformer providing ::ObviousFunctionsDocumentableFilterTransformer
    }

    val inheritedEntriesVisbilityFilter by extending {
        preMergeDocumentableTransformer providing ::InheritedEntriesDocumentableFilterTransformer
    }

    val emptyPackagesFilter by extending {
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

    val emptyModulesFilter by extending {
        preMergeDocumentableTransformer with EmptyModulesFilterTransformer() order {
            after(emptyPackagesFilter)
        }
    }

    val modulesAndPackagesDocumentation by extending {
        preMergeDocumentableTransformer providing ::ModuleAndPackageDocumentationTransformer
    }

    val actualTypealiasAdder by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    val kotlinSignatureProvider by extending {
        signatureProvider providing ::KotlinSignatureProvider
    }

    val sinceKotlinTransformer by extending {
        CoreExtensions.documentableTransformer providing ::SinceKotlinTransformer
    }

    val inheritorsExtractor by extending {
        CoreExtensions.documentableTransformer with InheritorsExtractorTransformer()
    }

    val undocumentedCodeReporter by extending {
        CoreExtensions.documentableTransformer with ReportUndocumentedTransformer()
    }

    val extensionsExtractor by extending {
        CoreExtensions.documentableTransformer with ExtensionExtractorTransformer()
    }

    val documentableToPageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing ::DefaultDocumentableToPageTranslator
    }

    val docTagToContentConverter by extending {
        commentsToContentConverter with DocTagToContentConverter()
    }

    val pageMerger by extending {
        CoreExtensions.pageTransformer providing ::PageMerger
    }

    val sourceSetMerger by extending {
        CoreExtensions.pageTransformer providing ::SourceSetMergingPageTransformer
    }

    val fallbackMerger by extending {
        pageMergerStrategy providing { ctx -> FallbackPageMergerStrategy(ctx.logger) }
    }

    val sameMethodNameMerger by extending {
        pageMergerStrategy providing { ctx -> SameMethodNamePageMergerStrategy(ctx.logger) } order {
            before(fallbackMerger)
        }
    }

    val defaultTabSortingStrategy by extending {
        tabSortingStrategy with DefaultTabSortingStrategy()
    }

    val htmlRenderer by extending {
        CoreExtensions.renderer providing ::HtmlRenderer
    }

    val defaultKotlinAnalysis by extending {
        kotlinAnalysis providing { ctx -> KotlinAnalysis(ctx.configuration.sourceSets, ctx.logger) }
    }

    val locationProvider by extending {
        locationProviderFactory providing ::DokkaLocationProviderFactory
    }

    val javadocLocationProvider by extending {
        externalLocationProviderFactory providing ::JavadocExternalLocationProviderFactory
    }

    val dokkaLocationProvider by extending {
        externalLocationProviderFactory providing ::DefaultExternalLocationProviderFactory
    }

    val fileWriter by extending {
        outputWriter providing ::FileWriter
    }

    val rootCreator by extending {
        htmlPreprocessors with RootCreator applyIf { !delayTemplateSubstitution }
    }

    val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer order {
            before(pageMerger)
        }
    }

    val sourceLinksTransformer by extending {
        htmlPreprocessors providing ::SourceLinksTransformer order { after(rootCreator) }
    }

    val navigationPageInstaller by extending {
        htmlPreprocessors providing ::NavigationPageInstaller order { after(rootCreator) }
    }

    val navigationSearchInstaller by extending {
        htmlPreprocessors providing ::NavigationSearchInstaller order { after(rootCreator) }
    }

    val scriptsInstaller by extending {
        htmlPreprocessors providing ::ScriptsInstaller order { after(rootCreator) }
    }

    val stylesInstaller by extending {
        htmlPreprocessors providing ::StylesInstaller order { after(rootCreator) }
    }

    val assetsInstaller by extending {
        htmlPreprocessors with AssetsInstaller order { after(rootCreator) } applyIf { !delayTemplateSubstitution }
    }

    val customResourceInstaller by extending {
        htmlPreprocessors providing { ctx -> CustomResourceInstaller(ctx) } order {
            after(stylesInstaller)
            after(scriptsInstaller)
            after(assetsInstaller)
        } applyIf { !delayTemplateSubstitution }
    }

    val packageListCreator by extending {
        htmlPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaHtml)
        } order { after(rootCreator) }
    }

    val sourcesetDependencyAppender by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }

    val resolveLinkConsumer by extending {
        immediateHtmlCommandConsumer with ResolveLinkConsumer
    }

    val pathToRootConsumer by extending {
        immediateHtmlCommandConsumer with PathToRootConsumer
    }
    val baseSearchbarDataInstaller by extending {
        htmlPreprocessors providing ::SearchbarDataInstaller order { after(sourceLinksTransformer) }
    }
}
