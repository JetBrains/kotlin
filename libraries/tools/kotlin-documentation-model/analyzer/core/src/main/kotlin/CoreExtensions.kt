package org.jetbrains.dokka

import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.validity.PreGenerationChecker
import kotlin.reflect.KProperty

object CoreExtensions {

    val preGenerationCheck by coreExtensionPoint<PreGenerationChecker>()
    val generation by coreExtensionPoint<Generation>()
    val sourceToDocumentableTranslator by coreExtensionPoint<SourceToDocumentableTranslator>()
    val documentableMerger by coreExtensionPoint<DocumentableMerger>()
    val documentableTransformer by coreExtensionPoint<DocumentableTransformer>()
    val documentableToPageTranslator by coreExtensionPoint<DocumentableToPageTranslator>()
    val pageTransformer by coreExtensionPoint<PageTransformer>()
    val renderer by coreExtensionPoint<Renderer>()

    private fun <T : Any> coreExtensionPoint() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }
}