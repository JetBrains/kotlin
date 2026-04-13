/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

@KotlinGradlePluginPublicDsl
interface KotlinTargetContainerWithJsPresetFunctions :
    KotlinTargetContainerWithPresetFunctions,
    KotlinJsCompilerTypeHolder {
    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Use js(name, configure) instead. Scheduled for removal in Kotlin 2.6.",
        replaceWith = ReplaceWith("js(name, configure)"),
        level = DeprecationLevel.WARNING,
    )
    fun js(
        name: String = DEFAULT_JS_NAME,
        compiler: KotlinJsCompilerType = defaultJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl

    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Use js(name, configure) instead. Scheduled for removal in Kotlin 2.6.",
        replaceWith = ReplaceWith("js(name, configure)"),
        level = DeprecationLevel.WARNING,
    )
    fun js(
        name: String,
        compiler: String,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(
        name = name,
        compiler = KotlinJsCompilerType.byArgument(compiler),
        configure = configure
    )

    @Suppress("DEPRECATION")
    fun js(
        name: String = DEFAULT_JS_NAME,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(
        name = name,
        compiler = KotlinJsCompilerType.IR,
        configure = configure
    )

    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Use js(configure) instead. Scheduled for removal in Kotlin 2.6.",
        replaceWith = ReplaceWith("js(configure)"),
        level = DeprecationLevel.WARNING,
    )
    fun js(
        compiler: KotlinJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(name = DEFAULT_JS_NAME, compiler = compiler, configure = configure)

    fun js() = js(name = DEFAULT_JS_NAME) { }
    fun js(name: String) = js(name = name) { }
    fun js(name: String, configure: Action<KotlinJsTargetDsl>) = js(name = name) { configure.execute(this) }
    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Use js(configure) instead. Scheduled for removal in Kotlin 2.6.",
        replaceWith = ReplaceWith("js(configure)"),
        level = DeprecationLevel.WARNING,
    )
    fun js(compiler: KotlinJsCompilerType, configure: Action<KotlinJsTargetDsl>) = js(compiler = compiler) {
        configure.execute(this)
    }

    fun js(configure: Action<KotlinJsTargetDsl>) = js { configure.execute(this) }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Use js(name, configure) instead. Scheduled for removal in Kotlin 2.6.",
        replaceWith = ReplaceWith("js(name, configure)"),
        level = DeprecationLevel.WARNING,
    )
    fun js(
        name: String,
        compiler: KotlinJsCompilerType,
        configure: Action<KotlinJsTargetDsl>
    ) = js(
        name = name,
        compiler = compiler
    ) {
        configure.execute(this)
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Kotlin/JS IR is the only supported compiler type. Use js(name, configure) instead. Scheduled for removal in Kotlin 2.6.",
        replaceWith = ReplaceWith("js(name, configure)"),
        level = DeprecationLevel.WARNING,
    )
    fun js(
        name: String,
        compiler: String,
        configure: Action<KotlinJsTargetDsl>
    ) = js(
        name = name,
        compiler = compiler
    ) {
        configure.execute(this)
    }

    @InternalKotlinGradlePluginApi
    companion object {
        internal const val DEFAULT_JS_NAME = "js"
    }
}
