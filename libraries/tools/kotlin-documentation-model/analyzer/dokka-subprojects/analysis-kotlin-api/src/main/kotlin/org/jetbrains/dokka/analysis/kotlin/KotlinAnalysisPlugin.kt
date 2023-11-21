/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin

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

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
