/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.*
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.attributesConfigurationHelper
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.runKotlinCompilationSideEffects
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.toAttribute
import org.jetbrains.kotlin.gradle.targets.runKotlinTargetSideEffects
import org.jetbrains.kotlin.gradle.utils.*

interface KotlinTargetConfigurator<KotlinTargetType : KotlinTarget> {
    fun configureTarget(
        target: KotlinTargetType,
    ) {
        target.runKotlinCompilationSideEffects()
        target.runKotlinTargetSideEffects()
    }
}

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    internal val createTestCompilation: Boolean,
) : KotlinTargetConfigurator<KotlinTargetType> {
    companion object {
        const val testTaskNameSuffix = "test"
        const val runTaskNameSuffix = "run"
    }
}

internal val KotlinTarget.testTaskName: String
    get() = lowerCamelCaseName(targetName, AbstractKotlinTargetConfigurator.testTaskNameSuffix)

abstract class KotlinOnlyTargetConfigurator<KotlinCompilationType : KotlinCompilation<*>, KotlinTargetType : KotlinOnlyTarget<KotlinCompilationType>>(
    createTestCompilation: Boolean,
) : AbstractKotlinTargetConfigurator<KotlinTargetType>(createTestCompilation)

internal fun Project.usageByName(usageName: String): Usage =
    objects.named(Usage::class.java, usageName)

internal fun Project.categoryByName(categoryName: String): Category =
    objects.named(Category::class.java, categoryName)

internal inline fun <reified T : Named> Project.attributeValueByName(attributeValueName: String): T =
    objects.named(T::class.java, attributeValueName)

fun Configuration.usesPlatformOf(
    target: KotlinTarget,
    jvmTargetProvider: (() -> JvmTarget)? = null,
) = setUsesPlatformOf(target, jvmTargetProvider)

internal fun <T : HasAttributes> T.setUsesPlatformOf(
    target: KotlinTarget,
    jvmTargetProvider: (() -> JvmTarget)? = null,
): T {
    attributes.setAttribute(KotlinPlatformType.attribute, target.platformType)

    when (target.platformType) {
        KotlinPlatformType.jvm -> setJvmSpecificAttributes(target, "standard-jvm", jvmTargetProvider)
        KotlinPlatformType.androidJvm -> setJvmSpecificAttributes(target, "android", jvmTargetProvider)
        /**
         *  We set this attribute even for non-JVM-like targets (JS, Native) to avoid issues with Gradle variant-aware dependency resolution
         *  treating variants which don't have a particular attribute more preferable than those having it in those cases when Gradle failed
         *  to choose the best match by biggest compatible attributes set (by inclusion). Having an attribute not
         *  set on some variants might break if there appears one more third-party attribute such that:
         *      * it is not set on some variants;
         *      * according to the other attributes which are set on all variants, there are both compatible candidate variants
         *        which have this attribute and those which don't;
         *  Note that this attribute is not published to avoid issues with older Kotlin versions combined with newer Gradle
         *  see [org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinUsageContext.filterOutNonPublishableAttributes]
         */
        else -> setJavaTargetEnvironmentAttributeIfSupported(target.project, "non-jvm")
    }

    val publishJsCompilerAttribute = target.project.kotlinPropertiesProvider.publishJsCompilerAttribute

    if (publishJsCompilerAttribute && target is KotlinJsIrTarget) {
        if (target.platformType == KotlinPlatformType.js) {
            attributes.setAttribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        } else {
            attributes.setAttributeProvider<KotlinWasmTargetAttribute>(
                target.project,
                KotlinWasmTargetAttribute.wasmTargetAttribute,
            ) { target.wasmTargetType!!.toAttribute() }
        }
    }

    if (target is KotlinNativeTarget) {
        attributes.setAttribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }

    return this
}

private fun HasAttributes.setJvmSpecificAttributes(
    target: KotlinTarget,
    targetJvmEnvironment: String,
    jvmTargetProvider: (() -> JvmTarget)?,
) {
    setJavaTargetEnvironmentAttributeIfSupported(target.project, targetJvmEnvironment)
    if (GradleVersion.current() >= GradleVersion.version("7.5")) {
        setTargetJvmAttribute(target.project, jvmTargetProvider)
    } else {
        // Postpone setting the attribute as the old Gradle releases do not support lazy value for attributes.
        // We do not do this unconditionally as such logic is actually more confusing in terms of visible side effects.
        // Once the minimal Gradle version becomes >= 7.5, this branch can be deleted.
        target.project.launchInStage(KotlinPluginLifecycle.Stage.FinaliseCompilations) {
            setTargetJvmAttribute(target.project, jvmTargetProvider)
        }
    }
}

private fun HasAttributes.setJavaTargetEnvironmentAttributeIfSupported(project: Project, value: String) {
    if (GradleVersion.current() >= GradleVersion.version("7.0")) {
        attributes.setAttribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            project.objects.named(value)
        )
    }
}

internal val KotlinCompilation<*>.jvmTargetProvider: () -> JvmTarget
    get() = {
        compileTaskProvider.flatMap {
            (it.compilerOptions as? KotlinJvmCompilerOptions)?.jvmTarget ?: error("JVM target is not applicable to this target")
        }.get()
    }

private fun HasAttributes.setTargetJvmAttribute(
    project: Project,
    jvmTargetProvider: (() -> JvmTarget)?,
) {
    /**
     * While this function is certainly used for configuring JVM targets,
     * it's important to note that in certain scenarios, such as publishing sources,
     * specifying a JVM target version might not be applicable or meaningful.
     * That's why we allow just skipping this attribute by null value
     */
    if (jvmTargetProvider == null) return

    project.attributesConfigurationHelper.setAttribute(
        attributes,
        TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE,
    ) {
        when (val jvmTarget = jvmTargetProvider()) {
            JvmTarget.JVM_1_8 -> 8
            else -> jvmTarget.target.toInt()
        }
    }
}

internal val Project.commonKotlinPluginClasspath get() = configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
