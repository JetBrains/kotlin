/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.CompletableFuture
import org.jetbrains.kotlin.gradle.utils.lenient

@Suppress("unused")
@Deprecated(
    message = "Still used by JB compose plugin",
    level = DeprecationLevel.HIDDEN
)
abstract class KotlinJsProjectExtension(project: Project) :
    KotlinSingleTargetExtension<KotlinJsTargetDsl>(project),
    KotlinJsCompilerTypeHolder {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    override val targetFuture = CompletableFuture<KotlinJsTargetDsl>().also {
        it.complete(project.objects.KotlinJsIrTarget(project, KotlinPlatformType.js))
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    override val target: KotlinJsTargetDsl
        get() = (this as KotlinSingleTargetExtension<KotlinJsTargetDsl>).targetFuture.lenient.getOrThrow()

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun registerTargetObserver(
        @Suppress("UNUSED_PARAMETER")
        observer: (KotlinJsTargetDsl?) -> Unit
    ) = Unit

    @Suppress("DEPRECATION_ERROR")
    private fun jsInternal(
        @Suppress("UNUSED_PARAMETER")
        body: KotlinJsTargetDsl.() -> Unit,
    ): KotlinJsTargetDsl {
        return (this as KotlinSingleTargetExtension<KotlinJsTargetDsl>).target
    }

    @Suppress("DEPRECATION")
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js(
        @Suppress("UNUSED_PARAMETER") // KT-64275
        compiler: KotlinJsCompilerType = defaultJsCompilerType,
        body: KotlinJsTargetDsl.() -> Unit = { },
    ): KotlinJsTargetDsl = jsInternal(body)

    @Suppress("DEPRECATION")
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js(
        compiler: String,
        body: KotlinJsTargetDsl.() -> Unit = { },
    ): KotlinJsTargetDsl = jsInternal(body)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js(
        body: KotlinJsTargetDsl.() -> Unit = { },
    ) = jsInternal(body = body)

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js() = jsInternal { }

    @Suppress("DEPRECATION")
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js(compiler: KotlinJsCompilerType, configure: Action<KotlinJsTargetDsl>) = jsInternal {
        configure.execute(this)
    }

    @Suppress("DEPRECATION")
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js(compiler: String, configure: Action<KotlinJsTargetDsl>) = jsInternal {
        configure.execute(this)
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun js(configure: Action<KotlinJsTargetDsl>) = jsInternal {
        configure.execute(this)
    }

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun getTargets(): NamedDomainObjectContainer<KotlinTarget>? = null
}
