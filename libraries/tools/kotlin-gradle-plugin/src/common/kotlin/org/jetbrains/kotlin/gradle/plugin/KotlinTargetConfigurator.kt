/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.*
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.runKotlinCompilationSideEffects
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.toAttribute
import org.jetbrains.kotlin.gradle.targets.runKotlinTargetSideEffects
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

interface KotlinTargetConfigurator<KotlinTargetType : KotlinTarget> {
    fun configureTarget(
        target: KotlinTargetType,
    ) {
        target.runKotlinCompilationSideEffects()
        target.runKotlinTargetSideEffects()
        configureBuild(target)
        configurePlatformSpecificModel(target)
    }

    fun configureBuild(target: KotlinTargetType)
    fun configurePlatformSpecificModel(target: KotlinTargetType) = Unit
}

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    internal val createTestCompilation: Boolean,
) : KotlinTargetConfigurator<KotlinTargetType> {

    protected open val runtimeIncludesCompilationOutputs = true

    override fun configureBuild(target: KotlinTargetType) {
        val project = target.project

        val buildNeeded = project.tasks.named(JavaBasePlugin.BUILD_NEEDED_TASK_NAME)
        val buildDependent = project.tasks.named(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            if (testCompilation is KotlinCompilationToRunnableFiles) {
                addDependsOnTaskInOtherProjects(project, buildNeeded, true, testCompilation.runtimeDependencyConfigurationName)
                addDependsOnTaskInOtherProjects(project, buildDependent, false, testCompilation.runtimeDependencyConfigurationName)
            }
        }
    }

    private fun addDependsOnTaskInOtherProjects(
        project: Project,
        taskProvider: TaskProvider<*>,
        useDependedOn: Boolean,
        configurationName: String,
    ) {
        val configuration = project.configurations.getByName(configurationName)
        taskProvider.configure { task ->
            task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, taskProvider.name))
        }
    }

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

internal interface KotlinTargetWithTestsConfigurator<R : KotlinTargetTestRun<*>, T : KotlinTargetWithTests<*, R>>
    : KotlinTargetConfigurator<T> {

    override fun configureTarget(target: T) {
        super.configureTarget(target)
        configureTest(target)
    }

    val testRunClass: Class<R>

    fun createTestRun(name: String, target: T): R

    fun configureTest(target: T) {
        initializeTestRuns(target)
        target.testRuns.create(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME)
    }

    private fun initializeTestRuns(target: T) {
        val project = target.project

        val testRunsPropertyName = KotlinTargetWithTests<*, *>::testRuns.name
        val mutableProperty =
            target::class.memberProperties
                .find { it.name == testRunsPropertyName } as? KMutableProperty1<*, *>
                ?: error(
                    "The ${this::class.qualifiedName} implementation of ${KotlinTargetWithTests::class.qualifiedName} must " +
                            "override the $testRunsPropertyName property with a var."
                )

        val testRunsContainer = project.container(testRunClass) { testRunName -> createTestRun(testRunName, target) }

        @Suppress("UNCHECKED_CAST")
        (mutableProperty as KMutableProperty1<KotlinTargetWithTests<*, R>, NamedDomainObjectContainer<R>>)
            .set(target, testRunsContainer)

        (target as ExtensionAware).extensions.add(target::testRuns.name, testRunsContainer)
    }
}

internal fun Project.usageByName(usageName: String): Usage =
    objects.named(Usage::class.java, usageName)

internal fun Project.categoryByName(categoryName: String): Category =
    objects.named(Category::class.java, categoryName)

internal inline fun <reified T : Named> Project.attributeValueByName(attributeValueName: String): T =
    objects.named(T::class.java, attributeValueName)

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)

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

    val publishJsCompilerAttribute = PropertiesProvider(target.project).publishJsCompilerAttribute

    if (publishJsCompilerAttribute && target is KotlinJsIrTarget) {
        if (target.platformType == KotlinPlatformType.js) {
            attributes.attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        } else {
            attributes.attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, target.wasmTargetType!!.toAttribute())
        }
    }

    // TODO: Provide an universal way to copy attributes from the target.
    if (target is KotlinNativeTarget) {
        attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }
    return this
}

private fun Configuration.setJavaTargetEnvironmentAttributeIfSupported(project: Project, value: String) {
    if (isGradleVersionAtLeast(7, 0)) {
        @Suppress("UNCHECKED_CAST")
        val attributeClass = Class.forName("org.gradle.api.attributes.java.TargetJvmEnvironment") as Class<out Named>

        @Suppress("UNCHECKED_CAST")
        val attributeKey = attributeClass.getField("TARGET_JVM_ENVIRONMENT_ATTRIBUTE").get(null) as Attribute<Named>

        val attributeValue = project.objects.named(attributeClass, value)
        attributes.attribute(attributeKey, attributeValue)
    }
}

internal val Project.commonKotlinPluginClasspath get() = configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
