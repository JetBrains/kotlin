package org.jetbrains.dokka.kotlinlang

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.plugability.DokkaPlugin

class SamplesTransformerPlugin : DokkaPlugin() {
    private val dokkaBase by lazy { plugin<DokkaBase>() }

    @Suppress("unused")
    val kotlinWebsiteSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::KotlinWebsiteSamplesTransformer override dokkaBase.defaultSamplesTransformer order {
            before(dokkaBase.pageMerger)
        }
    }
}