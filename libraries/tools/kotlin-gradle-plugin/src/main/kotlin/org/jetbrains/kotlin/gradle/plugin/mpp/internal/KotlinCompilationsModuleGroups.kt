/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain

/** This is a disjoint-set union-like approach to having a module name that is equal across associated compilations, as the compiler
 * now requires that to properly compile internal calls to friend classes. Associating a compilation with another one leads to their
 * disjoint sets being united, so their module names become equal, taken from the new leader compilation.
 *
 * TODO: once the compiler is able to correctly generate calls to internals in other modules, remove this logic.
 */
internal class KotlinCompilationsModuleGroups {
    private val moduleLeaderCompilationMap: MutableMap<KotlinCompilation<*>, KotlinCompilation<*>> =
        mutableMapOf()

    fun getModuleLeader(compilation: KotlinCompilation<*>): KotlinCompilation<*> {
        if (compilation !in moduleLeaderCompilationMap) {
            moduleLeaderCompilationMap[compilation] = compilation
        }
        val leader = moduleLeaderCompilationMap.getValue(compilation)
        return when {
            leader == compilation -> leader
            else -> getModuleLeader(leader).also { moduleLeaderCompilationMap[compilation] = it }
        }
    }

    fun unionModules(compilationA: KotlinCompilation<*>, compilationB: KotlinCompilation<*>) {
        val aLeader = getModuleLeader(compilationA)
        val bLeader = getModuleLeader(compilationB)

        if (aLeader == bLeader)
            return

        listOf(aLeader, bLeader).run {
            /** heuristically choose the new leader: choose `main` when possible, don't choose `*test*` when there's an alternative,
             * if that didn't work, choose the first name lexicographically */
            val newLeader = singleOrNull { it.isMain() }
                ?: singleOrNull { it.name.contains("main", true) }
                ?: singleOrNull { !it.name.contains("test", true) }
                ?: minByOrNull { it.name }!!

            forEach { moduleLeaderCompilationMap[it] = newLeader }
        }
    }

    companion object {
        private const val EXT_NAME = "kotlin.compilations.moduleGroups"

        fun getModuleLeaderCompilation(compilation: KotlinCompilation<*>): KotlinCompilation<*> =
            getInstance(compilation.target.project).getModuleLeader(compilation)

        fun unionModules(compilationA: KotlinCompilation<*>, compilationB: KotlinCompilation<*>) {
            getInstance(compilationA.target.project).unionModules(compilationA, compilationB)
        }

        private fun getInstance(project: Project): KotlinCompilationsModuleGroups {
            val ext = project.extensions.getByType(ExtraPropertiesExtension::class.java)
            if (!ext.has(EXT_NAME)) {
                ext.set(EXT_NAME, KotlinCompilationsModuleGroups())
            }
            @Suppress("UNCHECKED_CAST")
            return ext.get(EXT_NAME) as KotlinCompilationsModuleGroups
        }
    }
}