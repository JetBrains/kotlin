/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.ThreadSafe
import kotlin.concurrent.withLock

/**
 * Caches mapping [KtFile] -> [FirFile] of module [moduleInfo]
 */
@ThreadSafe
internal abstract class ModuleFileCache {
    abstract val session: FirSession

    /**
     * Maps [ClassId] to corresponding classifiers
     * If classifier with required [ClassId] is not found in given module then map contains [Optional.EMPTY]
     */
    abstract val classifierByClassId: ConcurrentHashMap<ClassId, Optional<FirClassLikeDeclaration<*>>>

    /**
     * Maps [CallableId] to corresponding callable
     * If callable with required [CallableId]] is not found in given module then map contains emptyList
     */
    abstract val callableByCallableId: ConcurrentHashMap<CallableId, List<FirCallableSymbol<*>>>

    /**
     * @return [FirFile] by [file] if it was previously built or runs [createValue] otherwise
     * The [createValue] is run under the lock so [createValue] is executed at most once for each [KtFile]
     */
    abstract fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile

    abstract fun getContainerFirFile(declaration: FirDeclaration): FirFile?

    abstract fun getCachedFirFile(ktFile: KtFile): FirFile?

    abstract val firFileLockProvider: LockProvider<FirFile>

    inline fun <D : FirDeclaration, R> withReadLockOn(declaration: D, action: (D) -> R): R {
        val file = getContainerFirFile(declaration)
            ?: error("No fir file found for\n${declaration.render()}")
        return firFileLockProvider.withReadLock(file) { action(declaration) }
    }
}

internal class ModuleFileCacheImpl(override val session: FirSession) : ModuleFileCache() {
    private val ktFileToFirFile = ConcurrentHashMap<KtFile, FirFile>()

    override val classifierByClassId: ConcurrentHashMap<ClassId, Optional<FirClassLikeDeclaration<*>>> = ConcurrentHashMap()
    override val callableByCallableId: ConcurrentHashMap<CallableId, List<FirCallableSymbol<*>>> = ConcurrentHashMap()

    override fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile =
        ktFileToFirFile.computeIfAbsent(file) { createValue() }

    override fun getCachedFirFile(ktFile: KtFile): FirFile? = ktFileToFirFile[ktFile]

    override fun getContainerFirFile(declaration: FirDeclaration): FirFile? {
        val ktFile = declaration.psi?.containingFile as? KtFile ?: return null
        return getCachedFirFile(ktFile)
    }

    override val firFileLockProvider: LockProvider<FirFile> = LockProvider()
}
