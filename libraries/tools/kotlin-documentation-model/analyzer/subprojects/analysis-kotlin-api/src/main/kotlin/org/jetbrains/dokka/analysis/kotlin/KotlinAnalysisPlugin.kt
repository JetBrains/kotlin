package org.jetbrains.dokka.analysis.kotlin

import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

class KotlinAnalysisPlugin : DokkaPlugin() {

    /*
     * This is where stable public API will go.
     *
     * No stable public API for now.
     */

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
