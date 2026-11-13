/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.translation

import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.providers.extractDeclarations
import org.jetbrains.kotlin.kir.providers.kirModule
import org.jetbrains.kotlin.kir.providers.withSession
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.swiftimport.standalone.session.StandaloneKirSession

/**
 * Translates top-level functions of [this] module to KIR. All other declarations are ignored.
 */
internal fun SirModule.toKirModule(): KirModule {
    val kirSession = StandaloneKirSession(moduleToTranslate = this)
    return kirSession.withSession {
        kirSession.moduleToTranslate.kirModule().also { kirModule ->
            kirModule.declarations += kirSession.moduleToTranslate.declarations.asSequence().extractDeclarations()
        }
    }
}
