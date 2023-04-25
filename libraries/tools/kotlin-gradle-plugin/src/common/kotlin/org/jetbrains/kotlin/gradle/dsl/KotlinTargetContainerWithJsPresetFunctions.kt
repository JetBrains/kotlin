/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension.Companion.reportJsCompilerMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension.Companion.warnAboutDeprecatedCompiler
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.targets.js.calculateJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface KotlinTargetContainerWithJsPresetFunctions :
    KotlinTargetContainerWithPresetFunctions,
    KotlinJsCompilerTypeHolder {
    fun js(
        name: String = "js",
        compiler: KotlinJsCompilerType = defaultJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl = jsInternal(
        name,
        compiler,
        configure
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

    fun js(
        name: String = "js",
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = jsInternal(name = name, configure = configure)

    fun js(
        compiler: KotlinJsCompilerType,
        configure: KotlinJsTargetDsl.() -> Unit = { }
    ) = js(name = "js", compiler = compiler, configure = configure)

    fun js() = jsInternal(name = "js") { }
    fun js(name: String) = jsInternal(name = name) { }
    fun js(name: String, configure: Action<KotlinJsTargetDsl>) = jsInternal(name = name) { configure.execute(this) }
    fun js(compiler: KotlinJsCompilerType, configure: Action<KotlinJsTargetDsl>) = js(compiler = compiler) {
        configure.execute(this)
    }

    fun js(configure: Action<KotlinJsTargetDsl>) = jsInternal { configure.execute(this) }

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
}

private fun KotlinTargetContainerWithJsPresetFunctions.jsInternal(
    name: String = "js",
    compiler: KotlinJsCompilerType? = null,
    configure: KotlinJsTargetDsl.() -> Unit
): KotlinJsTargetDsl {
    val existingTarget = getExistingTarget(name, compiler)

    val kotlinJsCompilerType = (compiler
        ?: existingTarget?.calculateJsCompilerType())

    val compilerOrDefault = kotlinJsCompilerType
        ?: defaultJsCompilerType

    val targetName = getTargetName(name, compilerOrDefault)

    if (existingTarget != null) {
        val previousCompilerType = existingTarget.calculateJsCompilerType()
        check(compiler == null || previousCompilerType == compiler) {
            "You already registered Kotlin/JS target '$targetName' with another compiler: ${previousCompilerType.lowerName}"
        }
    }

    reportJsCompilerMode(compilerOrDefault)

    @Suppress("UNCHECKED_CAST")
    return configureOrCreate(
        targetName,
        presets.getByName(
            lowerCamelCaseName(
                "js",
                if (compilerOrDefault == KotlinJsCompilerType.LEGACY) null else compilerOrDefault.lowerName
            )
        ) as KotlinTargetPreset<KotlinJsTargetDsl>,
        configure
    ).also { target ->
        warnAboutDeprecatedCompiler(target.project, compilerOrDefault)
    }
}

// Try to find existing target with exact name
// and with append suffix Legacy in case when compiler for found target is BOTH,
// and removed suffix Legacy in case when current compiler is BOTH
private fun KotlinTargetContainerWithJsPresetFunctions.getExistingTarget(
    name: String,
    compiler: KotlinJsCompilerType?
): KotlinJsTargetDsl? {

    fun getPreviousTarget(
        targetName: String,
        currentBoth: Boolean
    ): KotlinJsTargetDsl? {
        val singleTarget = targets.findByName(
            targetName
        ) as KotlinJsTargetDsl?

        return singleTarget?.let {
            val previousCompiler = it.calculateJsCompilerType()
            if (compiler == KotlinJsCompilerType.BOTH && currentBoth || previousCompiler == KotlinJsCompilerType.BOTH && !currentBoth) {
                it
            } else null
        }
    }

    val targetNameCandidate = getTargetName(name, compiler)

    return targets.findByName(targetNameCandidate) as KotlinJsTargetDsl?
        ?: getPreviousTarget(targetNameCandidate.removeJsCompilerSuffix(KotlinJsCompilerType.LEGACY), true)
        ?: getPreviousTarget(lowerCamelCaseName(targetNameCandidate, KotlinJsCompilerType.LEGACY.lowerName), false)
}

private fun getTargetName(name: String, compiler: KotlinJsCompilerType?): String {
    return lowerCamelCaseName(
        name,
        if (compiler == KotlinJsCompilerType.BOTH) KotlinJsCompilerType.LEGACY.lowerName else null
    )
}
