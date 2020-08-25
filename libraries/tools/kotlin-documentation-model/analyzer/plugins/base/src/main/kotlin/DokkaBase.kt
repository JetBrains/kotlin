@file:Suppress("unused")

package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.KotlinAnalysis
import org.jetbrains.dokka.base.allModulePage.MultimodulePageCreator
import org.jetbrains.dokka.base.renderers.*
import org.jetbrains.dokka.base.renderers.html.*
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.base.signatures.KotlinSignatureProvider
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter
import org.jetbrains.dokka.base.transformers.pages.merger.*
import org.jetbrains.dokka.base.transformers.pages.samples.DefaultSamplesTransformer
import org.jetbrains.dokka.base.transformers.pages.sourcelinks.SourceLinksTransformer
import org.jetbrains.dokka.base.translators.descriptors.DefaultDescriptorToDocumentableTranslator
import org.jetbrains.dokka.base.translators.documentables.DefaultDocumentableToPageTranslator
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.base.translators.psi.DefaultPsiToDocumentableTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DokkaBase : DokkaPlugin() {
    val pageMergerStrategy by extensionPoint<PageMergerStrategy>()
    val commentsToContentConverter by extensionPoint<CommentsToContentConverter>()
    val signatureProvider by extensionPoint<SignatureProvider>()
    val locationProviderFactory by extensionPoint<LocationProviderFactory>()
    val externalLocationProviderFactory by extensionPoint<ExternalLocationProviderFactory>()
    val outputWriter by extensionPoint<OutputWriter>()
    val htmlPreprocessors by extensionPoint<PageTransformer>()
    val kotlinAnalysis by extensionPoint<KotlinAnalysis>()
    val tabSortingStrategy by extensionPoint<TabSortingStrategy>()


    val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing { ctx ->
            DefaultDescriptorToDocumentableTranslator(ctx.single(kotlinAnalysis))
        }
    }

    val psiToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing { ctx ->
            DefaultPsiToDocumentableTranslator(ctx.single(kotlinAnalysis))
        }
    }

    val documentableMerger by extending {
        CoreExtensions.documentableMerger with DefaultDocumentableMerger
    }

    val deprecatedDocumentableFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    val suppressedDocumentableFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::SuppressedDocumentableFilterTransformer
    }

    val documentableVisbilityFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    val emptyPackagesFilter by extending {
        CoreExtensions.preMergeDocumentableTransformer providing ::EmptyPackagesFilterTransformer order {
            after(deprecatedDocumentableFilter, suppressedDocumentableFilter, documentableVisbilityFilter)
        }
    }

    val actualTypealiasAdder by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    val modulesAndPackagesDocumentation by extending {
        CoreExtensions.preMergeDocumentableTransformer providing { ctx ->
            ModuleAndPackageDocumentationTransformer(
                ModuleAndPackageDocumentationReader(ctx, ctx.single(kotlinAnalysis))
            )
        }
    }

    val kotlinSignatureProvider by extending {
        signatureProvider providing { ctx ->
            KotlinSignatureProvider(ctx.single(commentsToContentConverter), ctx.logger)
        }
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
        CoreExtensions.documentableToPageTranslator providing { ctx ->
            DefaultDocumentableToPageTranslator(
                ctx.single(commentsToContentConverter),
                ctx.single(signatureProvider),
                ctx.logger
            )
        }
    }

    val docTagToContentConverter by extending {
        commentsToContentConverter with DocTagToContentConverter
    }

    val pageMerger by extending {
        CoreExtensions.pageTransformer providing { ctx -> PageMerger(ctx[pageMergerStrategy]) }
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
        kotlinAnalysis providing { ctx -> KotlinAnalysis(ctx) }
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
        htmlPreprocessors with RootCreator
    }

    val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer order {
            before(pageMerger)
        }
    }

    val sourceLinksTransformer by extending {
        htmlPreprocessors providing {
            SourceLinksTransformer(
                it,
                PageContentBuilder(
                    it.single(commentsToContentConverter),
                    it.single(signatureProvider),
                    it.logger
                )
            )
        } order { after(rootCreator) }
    }

    val navigationPageInstaller by extending {
        htmlPreprocessors with NavigationPageInstaller order { after(rootCreator) }
    }

    val searchPageInstaller by extending {
        htmlPreprocessors with SearchPageInstaller order { after(rootCreator) }
    }

    val resourceInstaller by extending {
        htmlPreprocessors with ResourceInstaller order { after(rootCreator) }
    }

    val styleAndScriptsAppender by extending {
        htmlPreprocessors with StyleAndScriptsAppender order { after(rootCreator) }
    }

    val packageListCreator by extending {
        htmlPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaHtml)
        } order { after(rootCreator) }
    }

    val sourcesetDependencyAppender by extending {
        htmlPreprocessors providing ::SourcesetDependencyAppender order { after(rootCreator) }
    }

    val allModulePageCreators by extending {
        CoreExtensions.allModulePageCreator providing {
            MultimodulePageCreator(it)
        }
    }
}
