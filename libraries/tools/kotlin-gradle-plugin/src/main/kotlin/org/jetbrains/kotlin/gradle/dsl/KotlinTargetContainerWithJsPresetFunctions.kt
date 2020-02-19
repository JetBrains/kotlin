package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.JsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerTypeHolder
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface KotlinTargetContainerWithJsPresetFunctions :
    KotlinTargetContainerWithPresetFunctions,
    KotlinJsCompilerTypeHolder {
    fun js(
        name: String = "js",
        compiler: JsCompilerType = defaultJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl =
        configureOrCreate(
            lowerCamelCaseName(name, if (compiler == JsCompilerType.both) JsCompilerType.legacy.name else null),
            presets.getByName(
                lowerCamelCaseName(
                    "js",
                    if (compiler == JsCompilerType.legacy) null else compiler.name
                )
            ) as KotlinTargetPreset<KotlinJsTargetDsl>,
            configure
        )

    fun js(
        name: String = "js",
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(name = name, compiler = defaultJsCompilerType, configure = configure)

    fun js(
        compiler: JsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(name = "js", compiler = compiler, configure = configure)

    fun js() = js(name = "js") { }
    fun js(name: String) = js(name = name) { }
    fun js(name: String, configure: Closure<*>) = js(name = name) { ConfigureUtil.configure(configure, this) }
    fun js(compiler: JsCompilerType, configure: Closure<*>) = js(compiler = compiler) { ConfigureUtil.configure(configure, this) }
    fun js(configure: Closure<*>) = js { ConfigureUtil.configure(configure, this) }
}