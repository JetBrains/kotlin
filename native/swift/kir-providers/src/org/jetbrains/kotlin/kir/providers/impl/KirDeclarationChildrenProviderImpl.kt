/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.impl

import org.jetbrains.kotlin.kir.KirDeclaration
import org.jetbrains.kotlin.kir.KirAvailability
import org.jetbrains.kotlin.kir.providers.KirChildrenProvider
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.kirAvailability
import org.jetbrains.kotlin.kir.providers.toKir
import org.jetbrains.kotlin.kir.providers.withSession
import org.jetbrains.kotlin.sir.SirDeclaration

public class KirDeclarationChildrenProviderImpl(private val kirSession: KirSession) : KirChildrenProvider {
    override fun Sequence<SirDeclaration>.extractDeclarations(): Sequence<KirDeclaration> =
        kirSession.withSession {
            filter { it.kirAvailability() is KirAvailability.Available }
                .flatMap { it.toKir().allDeclarations }
        }
}
