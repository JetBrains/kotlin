/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.ExtensionPoint
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement

/**
 * A plugin for internal use, has no stable public API and thus must not be used by third party,
 * external plugins. If you need any of the given API stabilized, please create an issue describing your use case.
 */
@InternalDokkaApi
public class InternalKotlinAnalysisPlugin : DokkaPlugin() {

    public val fullClassHierarchyBuilder: ExtensionPoint<FullClassHierarchyBuilder> by extensionPoint()

    public val syntheticDocumentableDetector: ExtensionPoint<SyntheticDocumentableDetector> by extensionPoint()

    public val moduleAndPackageDocumentationReader: ExtensionPoint<ModuleAndPackageDocumentationReader> by extensionPoint()

    public val kotlinToJavaService: ExtensionPoint<KotlinToJavaService> by extensionPoint()

    public val inheritanceBuilder: ExtensionPoint<InheritanceBuilder> by extensionPoint()

    public val externalDocumentablesProvider: ExtensionPoint<ExternalDocumentablesProvider> by extensionPoint()

    public val documentableSourceLanguageParser: ExtensionPoint<DocumentableSourceLanguageParser> by extensionPoint()

    public val sampleProviderFactory: ExtensionPoint<SampleProviderFactory> by extensionPoint()

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
