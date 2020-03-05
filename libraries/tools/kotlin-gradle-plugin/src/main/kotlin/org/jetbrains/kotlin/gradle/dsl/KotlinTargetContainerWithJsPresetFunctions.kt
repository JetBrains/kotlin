package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerTypeHolder
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.lowerName
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface KotlinTargetContainerWithJsPresetFunctions :
    KotlinTargetContainerWithPresetFunctions,
    KotlinJsCompilerTypeHolder {
    fun js(
        name: String = "js",
        compiler: KotlinJsCompilerType = defaultJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl =
        configureOrCreate(
            lowerCamelCaseName(name, if (compiler == KotlinJsCompilerType.BOTH) KotlinJsCompilerType.LEGACY.lowerName else null),
            presets.getByName(
                lowerCamelCaseName(
                    "js",
                    if (compiler == KotlinJsCompilerType.LEGACY) null else compiler.lowerName
                )
            ) as KotlinTargetPreset<KotlinJsTargetDsl>,
            configure
        )

    fun js(
        name: String,
        compiler: String,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(
        name = name,
        compiler = KotlinJsCompilerType.byArgument(compiler)
            ?: throw IllegalArgumentException(
                "Unable to find $compiler setting. Use [${KotlinJsCompilerType.values().toList().joinToString()}]"
            ),
        configure = configure
    )

    fun js(
        name: String = "js",
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(name = name, compiler = defaultJsCompilerType, configure = configure)

    fun js(
        compiler: KotlinJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(name = "js", compiler = compiler, configure = configure)

    fun js() = js(name = "js") { }
    fun js(name: String) = js(name = name) { }
    fun js(name: String, configure: Closure<*>) = js(name = name) { ConfigureUtil.configure(configure, this) }
    fun js(compiler: KotlinJsCompilerType, configure: Closure<*>) = js(compiler = compiler) { ConfigureUtil.configure(configure, this) }
    fun js(configure: Closure<*>) = js { ConfigureUtil.configure(configure, this) }

    fun js(
        name: String,
        compiler: KotlinJsCompilerType,
        configure: Closure<*>
    ) = js(
        name = name,
        compiler = compiler
    ) {
        ConfigureUtil.configure(configure, this)
    }

    fun js(
        name: String,
        compiler: String,
        configure: Closure<*>
    ) = js(
        name = name,
        compiler = compiler
    ) {
        ConfigureUtil.configure(configure, this)
    }
}