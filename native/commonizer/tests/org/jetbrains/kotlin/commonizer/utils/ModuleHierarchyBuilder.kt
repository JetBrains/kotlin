/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import org.jetbrains.kotlin.commonizer.utils.InlineSourceBuilder.ModuleBuilder

class ModuleHierarchyBuilder(
    val buildName: (String) -> String,
) {
    val map: MutableMap<String, InlineSourceBuilder.Module> = mutableMapOf()

    fun sourceSet(target: String, builder: ModuleBuilder.(target: String) -> Unit): InlineSourceBuilder.Module =
        ModuleBuilder()
            .apply { name = buildName(target) }
            .apply { builder(target) }
            .build().also { map[target] = it }

    fun InlineSourceBuilder.Module.refinedBySourceSet(target: String, builder: ModuleBuilder.(target: String) -> Unit) =
        sourceSet(target) {
            builder(target)
            refinesDependency(this@refinedBySourceSet)
        }
}

fun createModuleHierarchy(
    nameSourceSetsAs: (String) -> String,
    block: ModuleHierarchyBuilder.() -> Unit,
): Map<String, InlineSourceBuilder.Module> =
    ModuleHierarchyBuilder(nameSourceSetsAs).apply(block).map
