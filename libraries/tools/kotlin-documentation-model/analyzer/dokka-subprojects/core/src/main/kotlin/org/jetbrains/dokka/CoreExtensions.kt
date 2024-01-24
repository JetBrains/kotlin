/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.generation.Generation
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.renderers.Renderer
import org.jetbrains.dokka.transformers.documentation.DocumentableMerger
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator
import org.jetbrains.dokka.validity.PreGenerationChecker
import kotlin.reflect.KProperty

public object CoreExtensions {

    public val preGenerationCheck: ExtensionPoint<PreGenerationChecker> by coreExtensionPoint<PreGenerationChecker>()

    public val generation: ExtensionPoint<Generation> by coreExtensionPoint<Generation>()

    public val sourceToDocumentableTranslator: ExtensionPoint<SourceToDocumentableTranslator> by coreExtensionPoint<SourceToDocumentableTranslator>()

    public val documentableMerger: ExtensionPoint<DocumentableMerger> by coreExtensionPoint<DocumentableMerger>()

    public val documentableTransformer: ExtensionPoint<DocumentableTransformer> by coreExtensionPoint<DocumentableTransformer>()

    public val documentableToPageTranslator: ExtensionPoint<DocumentableToPageTranslator> by coreExtensionPoint<DocumentableToPageTranslator>()

    public val pageTransformer: ExtensionPoint<PageTransformer> by coreExtensionPoint<PageTransformer>()

    public val renderer: ExtensionPoint<Renderer> by coreExtensionPoint<Renderer>()

    public val postActions: ExtensionPoint<PostAction> by coreExtensionPoint<PostAction>()

    private fun <T : Any> coreExtensionPoint() = object {
        operator fun provideDelegate(thisRef: CoreExtensions, property: KProperty<*>): Lazy<ExtensionPoint<T>> =
            lazy { ExtensionPoint(thisRef::class.qualifiedName!!, property.name) }
    }
}
