/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import com.intellij.openapi.util.Key
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.CompileContext
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

private val kotlinBuildTargetsCompileContextKey = Key<KotlinBuildTargets>("kotlinBuildTargets")

val CompileContext.kotlinBuildTargets: KotlinBuildTargets
    get() {
        val value = getUserData(kotlinBuildTargetsCompileContextKey)
        if (value != null) return value

        synchronized(this) {
            val actualValue = getUserData(kotlinBuildTargetsCompileContextKey)
            if (actualValue != null) return actualValue

            val newValue = KotlinBuildTargets(this)
            putUserData(kotlinBuildTargetsCompileContextKey, newValue)
            return newValue
        }
    }

class KotlinBuildTargets internal constructor(val compileContext: CompileContext) {
    private val byJpsModuleBuildTarget = ConcurrentHashMap<ModuleBuildTarget, KotlinModuleBuildTarget<*>>()
    private val isKotlinJsStdlibJar = ConcurrentHashMap<String, Boolean>()

    @JvmName("getNullable")
    operator fun get(target: ModuleBuildTarget?): KotlinModuleBuildTarget<*>? {
        if (target == null) return null
        return get(target)
    }

    operator fun get(target: ModuleBuildTarget): KotlinModuleBuildTarget<*>? {
        if (target.module.moduleType != JpsJavaModuleType.INSTANCE) return null

        return byJpsModuleBuildTarget.computeIfAbsent(target) {
            when (target.module.targetPlatform ?: detectTargetPlatform(target)) {
                is TargetPlatformKind.Common -> KotlinCommonModuleBuildTarget(compileContext, target)
                is TargetPlatformKind.JavaScript -> KotlinJsModuleBuildTarget(compileContext, target)
                is TargetPlatformKind.Jvm -> KotlinJvmModuleBuildTarget(compileContext, target)
            }
        }
    }

    /**
     * Compatibility for KT-14082
     * todo: remove when all projects migrated to facets
     */
    private fun detectTargetPlatform(target: ModuleBuildTarget): TargetPlatformKind<*> {
        if (hasJsStdLib(target)) return TargetPlatformKind.JavaScript

        return TargetPlatformKind.DEFAULT_PLATFORM
    }

    private fun hasJsStdLib(target: ModuleBuildTarget): Boolean {
        KotlinJvmModuleBuildTarget(compileContext, target).allDependencies.libraries.forEach { library ->
            for (root in library.getRoots(JpsOrderRootType.COMPILED)) {
                val url = root.url

                val isKotlinJsLib = isKotlinJsStdlibJar.computeIfAbsent(url) {
                    LibraryUtils.isKotlinJavascriptStdLibrary(JpsPathUtil.urlToFile(url))
                }

                if (isKotlinJsLib) return true
            }
        }

        return false
    }
}