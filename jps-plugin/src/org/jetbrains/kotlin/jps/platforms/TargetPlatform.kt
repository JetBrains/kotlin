/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.platforms

import com.intellij.openapi.util.Key
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.jps.model.platform
import org.jetbrains.kotlin.platform.DefaultIdeTargetPlatformKindProvider
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.impl.isJavaScript
import org.jetbrains.kotlin.platform.impl.isJvm
import org.jetbrains.kotlin.utils.LibraryUtils
import java.util.concurrent.ConcurrentHashMap

fun ModuleBuildTarget(module: JpsModule, isTests: Boolean) =
    ModuleBuildTarget(module, if (isTests) JavaModuleBuildTargetType.TEST else JavaModuleBuildTargetType.PRODUCTION)

val JpsModule.productionBuildTarget
    get() = ModuleBuildTarget(this, false)

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
        if (target.targetType !is ModuleBasedBuildTargetType) return null

        return byJpsModuleBuildTarget.computeIfAbsent(target) {
            val platform = target.module.platform?.kind ?: detectTargetPlatform(target)

            when {
                platform.isCommon -> KotlinCommonModuleBuildTarget(compileContext, target)
                platform.isJavaScript -> KotlinJsModuleBuildTarget(compileContext, target)
                platform.isJvm -> KotlinJvmModuleBuildTarget(compileContext, target)
                else -> error("Invalid platform $platform")
            }
        }
    }

    /**
     * Compatibility for KT-14082
     * todo: remove when all projects migrated to facets
     */
    private fun detectTargetPlatform(target: ModuleBuildTarget): IdePlatformKind<*> {
        if (hasJsStdLib(target)) {
            return JsIdePlatformKind
        }

        return DefaultIdeTargetPlatformKindProvider.defaultPlatform.kind
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