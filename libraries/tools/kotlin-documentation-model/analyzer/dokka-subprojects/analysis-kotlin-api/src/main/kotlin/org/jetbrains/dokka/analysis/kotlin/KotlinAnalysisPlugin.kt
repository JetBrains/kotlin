/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin

import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironmentCreator
import org.jetbrains.dokka.analysis.kotlin.sample.SampleAnalysisEnvironment
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

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
