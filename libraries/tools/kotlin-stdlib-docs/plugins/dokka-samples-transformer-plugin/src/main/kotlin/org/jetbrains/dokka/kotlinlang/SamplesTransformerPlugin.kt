package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.querySingle

class SamplesTransformerPlugin : DokkaPlugin() {
    @OptIn(InternalDokkaApi::class)
    private val dokkaKotlinAnalysisPlugin by lazy { plugin<InternalKotlinAnalysisPlugin>() }
    @OptIn(InternalDokkaApi::class)
    private val dokkaDescriptorAnalysisPlugin by lazy { plugin<CompilerDescriptorAnalysisPlugin>() }

    @OptIn(InternalDokkaApi::class)
    @Suppress("unused")
    val kotlinWebsiteSamplesTransformer by extending {
        dokkaKotlinAnalysisPlugin.sampleProviderFactory providing ::KotlinWebsiteSampleProviderFactory override dokkaDescriptorAnalysisPlugin.kotlinSampleProviderFactory
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}