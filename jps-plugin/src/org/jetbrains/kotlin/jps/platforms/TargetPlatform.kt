/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import org.jetbrains.annotations.TestOnly
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JpsJavaModuleType
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.jps.model.targetPlatform
import org.jetbrains.kotlin.utils.LibraryUtils
import java.util.concurrent.ConcurrentHashMap

fun ModuleBuildTarget(module: JpsModule, isTests: Boolean) =
    ModuleBuildTarget(module, if (isTests) JavaModuleBuildTargetType.TEST else JavaModuleBuildTargetType.PRODUCTION)

val JpsModule.productionBuildTarget
    get() = ModuleBuildTarget(this, false)

val JpsModule.testBuildTarget
    get() = ModuleBuildTarget(this, true)

private val kotlinBuildTargetsData = ConcurrentHashMap<ModuleBuildTarget, KotlinModuleBuilderTarget>()

val ModuleBuildTarget.kotlinData: KotlinModuleBuilderTarget?
    get() {
        if (module.moduleType != JpsJavaModuleType.INSTANCE) return null

        return kotlinBuildTargetsData.computeIfAbsent(this) {
            when (module.targetPlatform ?: detectTargetPlatform()) {
                is TargetPlatformKind.Common -> KotlinCommonModuleBuildTarget(this)
                is TargetPlatformKind.JavaScript -> KotlinJsModuleBuildTarget(this)
                is TargetPlatformKind.Jvm -> KotlinJvmModuleBuildTarget(this)
            }
        }
    }

/**
 * Compatibility for KT-14082
 * todo: remove when all projects migrated to facets
 */
private fun ModuleBuildTarget.detectTargetPlatform(): TargetPlatformKind<*> {
    if (hasJsStdLib()) return TargetPlatformKind.JavaScript

    return TargetPlatformKind.DEFAULT_PLATFORM
}

private val IS_KOTLIN_JS_STDLIB_JAR_CACHE = ConcurrentHashMap<String, Boolean>()

private fun ModuleBuildTarget.hasJsStdLib(): Boolean {
    KotlinJvmModuleBuildTarget(this).allDependencies.libraries.forEach { library ->
        for (root in library.getRoots(JpsOrderRootType.COMPILED)) {
            val url = root.url

            val isKotlinJsLib = IS_KOTLIN_JS_STDLIB_JAR_CACHE.computeIfAbsent(url) {
                LibraryUtils.isKotlinJavascriptStdLibrary(JpsPathUtil.urlToFile(url))
            }

            if (isKotlinJsLib) return true
        }
    }

    return false
}

@TestOnly
internal fun clearKotlinModuleBuildTargetDataBindings() {
    kotlinBuildTargetsData.clear()
}