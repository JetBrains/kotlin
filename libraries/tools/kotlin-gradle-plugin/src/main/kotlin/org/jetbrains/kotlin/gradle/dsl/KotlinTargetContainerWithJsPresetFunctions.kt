package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
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
        compiler = KotlinJsCompilerType.byArgument(compiler)
            ?: throw IllegalArgumentException(
                "Unable to find $compiler setting. Use [${KotlinJsCompilerType.values().toList().joinToString()}]"
            ),
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
    fun js(name: String, configure: Closure<*>) = jsInternal(name = name) { ConfigureUtil.configure(configure, this) }
    fun js(compiler: KotlinJsCompilerType, configure: Closure<*>) = js(compiler = compiler) { ConfigureUtil.configure(configure, this) }
    fun js(configure: Closure<*>) = jsInternal { ConfigureUtil.configure(configure, this) }

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

private fun KotlinTargetContainerWithJsPresetFunctions.jsInternal(
    name: String = "js",
    compiler: KotlinJsCompilerType? = null,
    configure: KotlinJsTargetDsl.() -> Unit
): KotlinJsTargetDsl {
    val existingTarget = getExistingTarget(name, compiler)

    val compilerOrDefault = compiler
        ?: existingTarget?.calculateJsCompilerType()
        ?: defaultJsCompilerType

    val targetName = getTargetName(name, compilerOrDefault)

    if (existingTarget != null) {
        val previousCompilerType = existingTarget.calculateJsCompilerType()
        check(compiler == null || previousCompilerType == compiler) {
            "You already registered Kotlin/JS target '$targetName' with another compiler: ${previousCompilerType.lowerName}"
        }
    }

    return configureOrCreate(
        targetName,
        presets.getByName(
            lowerCamelCaseName(
                "js",
                if (compilerOrDefault == KotlinJsCompilerType.LEGACY) null else compilerOrDefault.lowerName
            )
        ) as KotlinTargetPreset<KotlinJsTargetDsl>,
        configure
    )
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