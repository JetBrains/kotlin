/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin

import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
import org.jetbrains.dokka.analysis.kotlin.sample.SampleRewriter
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

public class KotlinAnalysisPlugin : DokkaPlugin() {

    /**
     * An extension for analyzing Kotlin sample functions used in the `@sample` KDoc tag.
     *
     * @see SampleAnalysisEnvironment for more details
     */
    public val sampleAnalysisEnvironmentCreator: ExtensionPoint<SampleAnalysisEnvironmentCreator> by extensionPoint()

    /**
     * An extension that helps to find external documentables that are not provided by Dokka by default,
     * such as documentables that come from external dependencies.
     *
     * @see ExternalDocumentableProvider for more details
     */
    public val externalDocumentableProvider: ExtensionPoint<ExternalDocumentableProvider> by extensionPoint()

    /**
     * An extensions can rewrite Kotlin sample functions that come from the `@sample` KDoc tag.
     * For example, it could be convenient to transform unit tests to samples.
     * Dokka supports no more than one rewriter. By default, Dokka provides no rewriter.
     *
     * @see SampleRewriter for more details
     * @see sampleAnalysisEnvironmentCreator
     */
    public val sampleRewriter: ExtensionPoint<SampleRewriter> by extensionPoint()

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
