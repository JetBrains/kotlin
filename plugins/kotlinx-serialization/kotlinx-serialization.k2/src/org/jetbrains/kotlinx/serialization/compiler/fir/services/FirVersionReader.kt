/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.utils.sourceElement
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.CommonVersionReader
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.RuntimeVersions
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames

class FirVersionReader(session: FirSession) : FirExtensionSessionComponent(session) {
    val runtimeVersions: RuntimeVersions? by session.firCachesFactory.createLazyValue lazy@{
        val markerClass = session.symbolProvider.getClassLikeSymbolByClassId(SerialEntityNames.KSERIALIZER_CLASS_ID) ?: return@lazy null
        CommonVersionReader.computeRuntimeVersions(markerClass.sourceElement)
    }

    val canSupportInlineClasses by session.firCachesFactory.createLazyValue lazy@{
        CommonVersionReader.canSupportInlineClasses(runtimeVersions)
    }
}

val FirSession.versionReader: FirVersionReader by FirSession.sessionComponentAccessor()

