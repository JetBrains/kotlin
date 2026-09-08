/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirParcelizeService(
    session: FirSession,
    val parcelizeAnnotations: List<ClassId>,
    val experimentalCodeGeneration: Boolean,
) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(parcelizeAnnotationFqNames: List<FqName>, experimentalCodeGeneration: Boolean): Factory {
            return Factory { session ->
                FirParcelizeService(
                    session = session,
                    parcelizeAnnotations = parcelizeAnnotationFqNames.map { ClassId.topLevel(it) },
                    experimentalCodeGeneration = experimentalCodeGeneration,
                )
            }
        }
    }
}

val FirSession.parcelizeService: FirParcelizeService by FirSession.sessionComponentAccessor()
