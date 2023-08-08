package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

/**
 * A plugin for internal use, has no stable public API and thus must not be used by third party,
 * external plugins. If you need any of the given API stabilized, please create an issue describing your use case.
 */
@InternalDokkaApi
class InternalKotlinAnalysisPlugin : DokkaPlugin() {

    val fullClassHierarchyBuilder by extensionPoint<FullClassHierarchyBuilder>()

    val syntheticDocumentableDetector by extensionPoint<SyntheticDocumentableDetector>()

    val moduleAndPackageDocumentationReader by extensionPoint<ModuleAndPackageDocumentationReader>()

    val kotlinToJavaService by extensionPoint<KotlinToJavaService>()

    val inheritanceBuilder by extensionPoint<InheritanceBuilder>()

    val externalDocumentablesProvider by extensionPoint<ExternalDocumentablesProvider>()

    val documentableSourceLanguageParser by extensionPoint<DocumentableSourceLanguageParser>()

    val sampleProviderFactory by extensionPoint<SampleProviderFactory>()

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
