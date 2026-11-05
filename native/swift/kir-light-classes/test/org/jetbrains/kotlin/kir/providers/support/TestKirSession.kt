/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.providers.support

import org.jetbrains.kir.lightclasses.KirDeclarationFromSirDeclarationProvider
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.providers.KirChildrenProvider
import org.jetbrains.kotlin.kir.providers.KirDeclarationNamer
import org.jetbrains.kotlin.kir.providers.KirDeclarationProvider
import org.jetbrains.kotlin.kir.providers.KirModuleProvider
import org.jetbrains.kotlin.kir.providers.KirParentProvider
import org.jetbrains.kotlin.kir.providers.KirSession
import org.jetbrains.kotlin.kir.providers.KirVisibilityChecker
import org.jetbrains.kotlin.kir.providers.extractDeclarations
import org.jetbrains.kotlin.kir.providers.impl.CachingKirDeclarationProvider
import org.jetbrains.kotlin.kir.providers.impl.KirDeclarationChildrenProviderImpl
import org.jetbrains.kotlin.kir.providers.impl.KirDeclarationNamerImpl
import org.jetbrains.kotlin.kir.providers.impl.KirOneToOneModuleProvider
import org.jetbrains.kotlin.kir.providers.impl.KirParentProviderImpl
import org.jetbrains.kotlin.kir.providers.impl.KirVisibilityCheckerImpl
import org.jetbrains.kotlin.kir.providers.kirModule
import org.jetbrains.kotlin.kir.providers.withSession
import org.jetbrains.kotlin.sir.SirModule

class TestKirSession(override val moduleToTranslate: SirModule) : KirSession {
    override val declarationNamer: KirDeclarationNamer = KirDeclarationNamerImpl()
    override val declarationProvider: KirDeclarationProvider = CachingKirDeclarationProvider(
        KirDeclarationFromSirDeclarationProvider(kirSession)
    )
    override val parentProvider: KirParentProvider = KirParentProviderImpl(kirSession)
    override val moduleProvider: KirModuleProvider = KirOneToOneModuleProvider()
    override val visibilityChecker: KirVisibilityChecker = KirVisibilityCheckerImpl()
    override val childrenProvider: KirChildrenProvider = KirDeclarationChildrenProviderImpl(kirSession)
}

/**
 * Translates [this] module with a fresh [TestKirSession], and passes both the result and the session to [block].
 */
fun <T> SirModule.translate(block: TestKirSession.(KirModule) -> T): T {
    val session = TestKirSession(this)
    val kirModule = session.withSession {
        this@translate.kirModule().also { module ->
            module.declarations += this@translate.declarations.asSequence().extractDeclarations()
        }
    }
    return session.block(kirModule)
}

fun KirModule.functionsNamed(name: String): List<KirFunction> =
    declarations.filterIsInstance<KirFunction>().filter { it.name == name }
