package org.jetbrains.dokka

import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.generation.SingleModuleGeneration
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.plugability.LazyEvaluated
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageCreator
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.validity.PreGenerationChecker
import kotlin.reflect.KProperty

object CoreExtensions {
    private val extensionDelegates = mutableListOf<Lazy<Extension<*, *, *>>>()

    val preGenerationCheck by coreExtensionPoint<PreGenerationChecker>()
    val generation by coreExtensionPoint<Generation>()
    val sourceToDocumentableTranslator by coreExtensionPoint<SourceToDocumentableTranslator>()
    val preMergeDocumentableTransformer by coreExtensionPoint<PreMergeDocumentableTransformer>()
    val documentableMerger by coreExtensionPoint<DocumentableMerger>()
    val documentableTransformer by coreExtensionPoint<DocumentableTransformer>()
    val documentableToPageTranslator by coreExtensionPoint<DocumentableToPageTranslator>()
    val allModulePageCreator by coreExtensionPoint<PageCreator>()
    val pageTransformer by coreExtensionPoint<PageTransformer>()
    val allModulePageTransformer by coreExtensionPoint<PageTransformer>()
    val renderer by coreExtensionPoint<Renderer>()

    val singleGeneration by generation extendWith LazyEvaluated.fromRecipe(::SingleModuleGeneration)

    private fun <T : Any> coreExtensionPoint() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint<T>(thisRef::class.qualifiedName!!, property.name) }
    }

    private infix fun <T: Any> ExtensionPoint<T>.extendWith(action: LazyEvaluated<T>) = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<Extension<T, OrderingKind.None, OverrideKind.None>> =
            lazy { Extension(this@extendWith, thisRef::class.qualifiedName!!, property.name, action) }
                .also { extensionDelegates += it }
    }

    internal fun installTo(context: DokkaContextConfiguration) {
        extensionDelegates.forEach {
            context.installExtension(it.value)
        }
    }
}