/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.name.FqName

class AllOpenStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    companion object {
        private val ALL_OPEN = FqName("org.jetbrains.kotlin.fir.plugin.AllOpen")
    }

    override fun transformStatus(declaration: FirDeclaration, status: FirDeclarationStatus): FirDeclarationStatus {
        if (status.modality != null) return status
        return status.transform(modality = Modality.OPEN)
    }

    override val mode: Mode
        get() = Mode.ANNOTATED_ELEMENT

    override val directlyApplicableAnnotations: Set<AnnotationFqn>
        get() = setOf(ALL_OPEN)

    override val childrenApplicableAnnotations: Set<AnnotationFqn>
        get() = setOf(ALL_OPEN)

    override val metaAnnotations: Map<AnnotationFqn, MetaAnnotationMode>
        get() = mapOf(ALL_OPEN to MetaAnnotationMode.ANNOTATED_AND_CHILDREN)

    override val key: FirPluginKey
        get() = AllOpenPluginKey
}