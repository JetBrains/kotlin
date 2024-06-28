package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class SamplesTransformerPlugin : DokkaPlugin() {
    private val kotlinAnalysisPlugin by lazy { plugin<KotlinAnalysisPlugin>() }

    @Suppress("unused")
    val kotlinWebsiteSamplesTransformer by extending {
        kotlinAnalysisPlugin.sampleRewriter providing ::KotlinWebsiteSampleRewriter
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
