/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * Dokka-internal plugin installed only by [runDokkaAnalysis], not visible to (and with no effect on) any other
 * Dokka consumer or test in this module.
 *
 * Suppresses `DokkaBase.jvmMappedMethodsFilter`. That filter intentionally hides members a class inherits via
 * Kotlin's [Java interop type mapping](https://kotlinlang.org/docs/java-interop.html#mapped-types) - e.g. a
 * `Throwable` subclass's JVM-only `printStackTrace`/`getStackTrace`/etc. - because Dokka's own HTML documentation
 * has no KDoc/Javadoc to show for them. KDM instead wants the full structural member list a declaration has on a
 * given platform, documented or not, so this plugin overrides that one filter with a no-op while leaving every
 * other part of Dokka's default pipeline untouched.
 */
internal class KdmDokkaPlugin : DokkaPlugin() {
    private object NoOpPreMergeDocumentableTransformer : PreMergeDocumentableTransformer {
        override fun invoke(modules: List<DModule>): List<DModule> = modules
    }

    @Suppress("unused")
    val suppressJvmMappedMethodsFilter by extending {
        val dokkaBase = plugin<DokkaBase>()
        dokkaBase.preMergeDocumentableTransformer with NoOpPreMergeDocumentableTransformer override dokkaBase.jvmMappedMethodsFilter
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
