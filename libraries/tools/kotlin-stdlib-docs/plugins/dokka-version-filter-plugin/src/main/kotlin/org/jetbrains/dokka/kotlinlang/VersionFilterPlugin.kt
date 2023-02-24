package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement


class VersionFilterPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    @Suppress("unused")
    val versionFilter by extending {
        CoreExtensions.documentableTransformer providing ::VersionFilterTransformer order {
            after(dokkaBase.sinceKotlinTransformer)
            before(dokkaBase.extensionsExtractor)
            before(dokkaBase.inheritorsExtractor)
        }
    }

    @DokkaPluginApiPreview
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement

}