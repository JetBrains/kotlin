/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.module.JpsModule

fun ModuleChunk.isDummy(context: CompileContext): Boolean {
    val targetIndex = context.projectDescriptor.buildTargetIndex
    return targets.all { targetIndex.isDummy(it) }
}

@Deprecated("Use `kotlin.targetBinding` instead", ReplaceWith("kotlin.targetsBinding"))
val CompileContext.kotlinBuildTargets
    get() = kotlin.targetsBinding

fun ModuleChunk.toKotlinChunk(context: CompileContext): KotlinChunk? =
    context.kotlin.getChunk(this)

fun ModuleBuildTarget(module: JpsModule, isTests: Boolean) =
    ModuleBuildTarget(
        module,
        if (isTests) JavaModuleBuildTargetType.TEST else JavaModuleBuildTargetType.PRODUCTION
    )

val JpsModule.productionBuildTarget
    get() = ModuleBuildTarget(this, false)

val JpsModule.testBuildTarget
    get() = ModuleBuildTarget(this, true)
