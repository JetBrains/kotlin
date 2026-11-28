/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.documentation.KdElementId

internal class KotlinArrayDocumentableReplacerTransformer(
    context: DokkaContext
): DocumentableReplacerTransformer(context) {

    private fun Documentable.isJVM() =
        sourceSets.any{ it.analysisPlatform == Platform.jvm }

    override fun processGenericTypeConstructor(genericTypeConstructor: GenericTypeConstructor): AnyWithChanges<GenericTypeConstructor> =
        genericTypeConstructor.takeIf { it.declarationId == KdElementId("kotlin/Array") }
            ?.let {
                with(it.projections.firstOrNull() as? Variance<Bound>) {
                    with(this?.inner as? GenericTypeConstructor) {
                        when (this?.declarationId) {
                            KdElementId("kotlin/Int") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/IntArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Boolean") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/BooleanArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Float") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/FloatArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Double") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/DoubleArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Long") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/LongArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Short") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/ShortArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Char") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/CharArray"), emptyList()),
                                    true)
                            KdElementId("kotlin/Byte") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(KdElementId("kotlin/ByteArray"), emptyList()),
                                    true)
                            else -> null
                        }
                    }
                }
            }
            ?: super.processGenericTypeConstructor(genericTypeConstructor)

    override fun processModule(module: DModule): AnyWithChanges<DModule>  =
        if(module.isJVM())
            super.processModule(module)
        else AnyWithChanges(module)
}
