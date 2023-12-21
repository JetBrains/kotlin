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
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
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

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.setAttribute(KotlinPlatformType.attribute, target.platformType)

    when (target.platformType) {
        KotlinPlatformType.jvm -> setJavaTargetEnvironmentAttributeIfSupported(target.project, "standard-jvm")
        KotlinPlatformType.androidJvm -> setJavaTargetEnvironmentAttributeIfSupported(target.project, "android")
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

private fun Configuration.setJavaTargetEnvironmentAttributeIfSupported(project: Project, value: String) {
    if (GradleVersion.current() >= GradleVersion.version("7.0")) {
        attributes.setAttribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            project.objects.named(value)
        )
    }
}

internal val Project.commonKotlinPluginClasspath get() = configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
