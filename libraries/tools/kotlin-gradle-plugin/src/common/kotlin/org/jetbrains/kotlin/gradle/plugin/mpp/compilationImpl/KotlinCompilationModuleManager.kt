/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager.CompilationModule
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.jetbrains.kotlin.tooling.core.withLinearClosure

/**
 * This is a disjoint-set union-like approach to having a module name that is equal across associated compilations, as the compiler
 * now requires that to properly compile internal calls to friend classes. Associating a compilation with another one leads to their
 * disjoint sets being united, so their module names become equal, taken from the new leader compilation.
 */
internal interface KotlinCompilationModuleManager {
    class CompilationModule(val compilationName: String, val ownModuleName: Provider<String>, val type: Type) {
        enum class Type {
            Main, /* Including tests */ Auxiliary
        }
    }

    fun unionModules(first: CompilationModule, second: CompilationModule)
    fun getModuleLeader(compilation: CompilationModule): CompilationModule
}

internal val Project.kotlinCompilationModuleManager: KotlinCompilationModuleManager
    get() = extensions.extraProperties.getOrPut(KotlinCompilationModuleManager::class.java.name) {
        DefaultKotlinCompilationModuleManager()
    }

private class DefaultKotlinCompilationModuleManager : KotlinCompilationModuleManager {
    private val leadingReferenceMap = mutableMapOf</*Follower compilationName*/String, /*Leader*/CompilationModule>()

    override fun getModuleLeader(compilation: CompilationModule): CompilationModule {
        return compilation.withLinearClosure { next -> leadingReferenceMap[next.compilationName] }.last()
    }

    override fun unionModules(first: CompilationModule, second: CompilationModule) {
        val firstLeader = getModuleLeader(first)
        val secondLeader = getModuleLeader(second)
        if (firstLeader == secondLeader) return

        val newLeaderCandidates = listOf(firstLeader, secondLeader)

        /*
        heuristically choose the new leader: choose `main` when possible, don't choose `*test*` when there's an alternative,
        if that didn't work, choose the first name lexicographically
        */
        val newLeader = newLeaderCandidates.singleOrNull { it.type == CompilationModule.Type.Main }
            ?: newLeaderCandidates.singleOrNull { it.compilationName.contains("main", true) }
            ?: newLeaderCandidates.singleOrNull { !it.compilationName.contains("test", true) }
            ?: checkNotNull(newLeaderCandidates.minByOrNull { it.compilationName })

        setOf(first, second, firstLeader, secondLeader).forEach { compilationInfo ->
            leadingReferenceMap[compilationInfo.compilationName] = newLeader
        }
    }
}