/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.swc

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.ES_2015
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.plugin.mpp.JsFileExtensions
import kotlin.collections.buildMap

internal enum class JsPlatformType { NODE, BROWSER, LIBRARY }

internal abstract class KotlinSwcConfig {
    @get:Input
    abstract val esTarget: Property<String>

    @get:Input
    abstract val sourceMaps: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val moduleKind: Property<JsModuleKind>

    @get:Input
    abstract val platformType: Property<JsPlatformType>

    private val moduleSystemToUse: Provider<JsModuleKind> =
        moduleKind
            .orElse(esTarget.zip(platformType) { esTarget, platformType ->
                when {
                    esTarget == ES_2015 -> JsModuleKind.MODULE_ES
                    platformType == JsPlatformType.NODE -> JsModuleKind.MODULE_COMMONJS
                    else -> JsModuleKind.MODULE_UMD
                }
            })

    private val swcModuleType: Provider<String> = moduleSystemToUse.map {
        if (it == JsModuleKind.MODULE_ES) "nodenext" else it.kind
    }

    private val parseMap: Provider<Boolean> = platformType.zip(sourceMaps) { platformType, sourceMaps ->
        platformType == JsPlatformType.BROWSER && sourceMaps
    }

    @get:Internal
    internal val fileExtension: Provider<String> =
        moduleSystemToUse.map { if (it == JsModuleKind.MODULE_ES) JsFileExtensions.MODULE else JsFileExtensions.REGULAR }

    fun toConfigMap(): Map<String, Any> = buildMap {
        set("\$schema", "https://swc.rs/schema.json")
        set("sourceMaps", sourceMaps.get())
        set("inputSourceMap", sourceMaps.get())
        if (parseMap.get()) set("parseMap", true)
        set("exclude", arrayOf(".*\\.d\\.m?ts$"))
        set("jsc", buildMap<String, Any> {
            set("parser", buildMap {
                set("syntax", "ecmascript")
                set("dynamicImport", true)
                set("functionBind", true)
                set("importMeta", true)
            })
            set("loose", true)
            set("externalHelpers", true)
            set("target", esTarget.get())
        })
        if (platformType.get() == JsPlatformType.NODE) {
            set("module", buildMap {
                set("resolveFully", true)
                set("type", swcModuleType.get())
                set("outFileExtension", fileExtension.get())
            })
        } else {
            set("isModule", "unknown")
        }
    }
}