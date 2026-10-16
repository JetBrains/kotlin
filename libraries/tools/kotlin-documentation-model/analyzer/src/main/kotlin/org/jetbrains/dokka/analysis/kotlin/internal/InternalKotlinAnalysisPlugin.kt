/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
internal class InternalKotlinAnalysisPlugin : DokkaPlugin() {

    val fullClassHierarchyBuilder: ExtensionPoint<FullClassHierarchyBuilder> by extensionPoint()

    val syntheticDocumentableDetector: ExtensionPoint<SyntheticDocumentableDetector> by extensionPoint()

    val moduleAndPackageDocumentationReader: ExtensionPoint<ModuleAndPackageDocumentationReader> by extensionPoint()

    val kotlinToJavaService: ExtensionPoint<KotlinToJavaService> by extensionPoint()

    val inheritanceBuilder: ExtensionPoint<InheritanceBuilder> by extensionPoint()

    val documentableSourceLanguageParser: ExtensionPoint<DocumentableSourceLanguageParser> by extensionPoint()

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
