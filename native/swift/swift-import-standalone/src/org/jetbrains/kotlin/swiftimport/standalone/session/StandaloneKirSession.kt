/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.session

import org.jetbrains.kir.lightclasses.KirDeclarationFromSirDeclarationProvider
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.impl.CachingKirDeclarationProvider
import org.jetbrains.kotlin.kir.providers.impl.KirDeclarationChildrenProviderImpl
import org.jetbrains.kotlin.kir.providers.impl.KirDeclarationNamerImpl
import org.jetbrains.kotlin.kir.providers.impl.KirOneToOneModuleProvider
import org.jetbrains.kotlin.kir.providers.impl.KirParentProviderImpl
import org.jetbrains.kotlin.kir.providers.impl.KirVisibilityCheckerImpl
import org.jetbrains.kotlin.sir.SirModule

internal class StandaloneKirSession(
    override val moduleToTranslate: SirModule,
) : KirSession {

    override val declarationNamer = KirDeclarationNamerImpl()

    override val declarationProvider = CachingKirDeclarationProvider(
        declarationsProvider = KirDeclarationFromSirDeclarationProvider(
            kirSession = kirSession,
        )
    )

    override val parentProvider = KirParentProviderImpl(kirSession)

    override val moduleProvider = KirOneToOneModuleProvider()

    override val visibilityChecker = KirVisibilityCheckerImpl()

    override val childrenProvider = KirDeclarationChildrenProviderImpl(kirSession)
}
