/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.generation.SingleModuleGeneration
import org.jetbrains.dokka.base.generation.SourceSetIdUniquenessChecker
import org.jetbrains.dokka.base.transformers.documentables.*
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.transformers.documentation.*

@Suppress("unused")
internal class DokkaBase : DokkaPlugin() {

    val preMergeDocumentableTransformer: ExtensionPoint<PreMergeDocumentableTransformer> by extensionPoint()

    val singleGeneration: Extension<Generation, *, *> by extending {
        CoreExtensions.generation providing ::SingleModuleGeneration
    }

    val documentableMerger: Extension<DocumentableMerger, *, *> by extending {
        CoreExtensions.documentableMerger providing ::DefaultDocumentableMerger
    }

    val deprecatedDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::DeprecatedDocumentableFilterTransformer
    }

    val suppressedDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::SuppressedByConfigurationDocumentableFilterTransformer
    }

    val suppressedByAnnotationsFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::SuppressedByAnnotationsDocumentableFilterTransformer order {
            after(suppressedDocumentableFilter)
        }
    }

    val suppressedBySuppressTagDocumentableFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::SuppressTagDocumentableFilter
    }

    val jvmMappedMethodsFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::JvmMappedMethodsDocumentableFilterTransformer order {
            before(kotlinArrayDocumentableReplacer)
        }
    }

    val documentableVisibilityFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::DocumentableVisibilityFilterTransformer
    }

    val obviousFunctionsVisbilityFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::ObviousFunctionsDocumentableFilterTransformer
    }

    val inheritedEntriesVisbilityFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::InheritedEntriesDocumentableFilterTransformer
    }

    val kotlinArrayDocumentableReplacer: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::KotlinArrayDocumentableReplacerTransformer
    }

    val emptyPackagesFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::EmptyPackagesFilterTransformer order {
            after(
                deprecatedDocumentableFilter,
                suppressedDocumentableFilter,
                suppressedByAnnotationsFilter,
                documentableVisibilityFilter,
                suppressedBySuppressTagDocumentableFilter,
                obviousFunctionsVisbilityFilter,
                inheritedEntriesVisbilityFilter,
            )
        }
    }

    val emptyModulesFilter: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer with EmptyModulesFilterTransformer() order {
            after(emptyPackagesFilter)
        }
    }

    val modulesAndPackagesDocumentation: Extension<PreMergeDocumentableTransformer, *, *> by extending {
        preMergeDocumentableTransformer providing ::ModuleAndPackageDocumentationTransformer order {
            after(emptyModulesFilter)
        }
    }

    val actualTypealiasAdder: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with ActualTypealiasAdder()
    }

    val sinceKotlinTransformer: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer providing ::SinceKotlinTransformer applyIf {
            DokkaBaseInternalConfiguration.sinceKotlinRenderingEnabled
        } order {
            before(extensionsExtractor)
        }
    }

    val inheritorsExtractor: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with InheritorsExtractorTransformer()
    }

    val undocumentedCodeReporter: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with ReportUndocumentedTransformer()
    }

    val extensionsExtractor: Extension<DocumentableTransformer, *, *> by extending {
        CoreExtensions.documentableTransformer with ExtensionExtractorTransformer()
    }

    internal val sourceSetIdUniquenessChecker by extending {
        CoreExtensions.preGenerationCheck providing ::SourceSetIdUniquenessChecker
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
        PluginApiPreviewAcknowledgement
}
