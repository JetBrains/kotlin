/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinNodeJs
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import javax.inject.Inject

open class KotlinJsTarget
@Inject
constructor(
    project: Project,
    platformType: KotlinPlatformType
) :
    KotlinOnlyTarget<KotlinJsCompilation>(project, platformType),
    KotlinTargetWithTests<JsAggregatingExecutionSource, KotlinJsReportAggregatingTestRun>,
    KotlinJsTargetDsl {
    override lateinit var testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun>
        internal set

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        if (irTarget == null)
            super.kotlinComponents
        else {
            val usageContexts = mutableSetOf<DefaultKotlinUsageContext>()

            // This usage value is only needed for Maven scope mapping. Don't replace it with a custom Kotlin Usage value
            val javaApiUsage = project.usageByName(if (isGradleVersionAtLeast(5, 3)) "java-api-jars" else Usage.JAVA_API)

            usageContexts += run {
                DefaultKotlinUsageContext(
                    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                    javaApiUsage,
                    apiElementsConfigurationName
                )
            }

            usageContexts += run {
                DefaultKotlinUsageContext(
                    irTarget!!.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME),
                    javaApiUsage,
                    irTarget!!.apiElementsConfigurationName
                )
            }

            val componentName =
                if (project.kotlinExtension is KotlinMultiplatformExtension)
                    targetName
                else PRIMARY_SINGLE_COMPONENT_NAME

            val component =
                createKotlinVariant(componentName, compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME), usageContexts)

            val sourcesJarTask =
                sourcesJarTask(project, lazy { project.kotlinExtension.sourceSets.toSet() }, null, targetName.toLowerCase())

            component.sourcesArtifacts = setOf(
                project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, sourcesJarTask).apply {
                    this as ConfigurablePublishArtifact
                    classifier = "sources"
                }
            )

            setOf(component)
        }
    }

    var irTarget: KotlinJsIrTarget? = null

    val testTaskName get() = testRuns.getByName(KotlinTargetWithTests.DEFAULT_TEST_RUN_NAME).testTaskName
    val testTask: TaskProvider<KotlinTestReport>
        get() = checkNotNull(project.locateTask(testTaskName))

    val runTaskName get() = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask
        get() = project.tasks.maybeCreate(runTaskName).also {
            it.description = "Run js on all configured platforms"
        }

    private val browserLazyDelegate = lazy {
        project.objects.newInstance(KotlinBrowserJs::class.java, this).also {
            it.configure()
            browserConfiguredHandlers.forEach { handler -> handler(it) }
            browserConfiguredHandlers.clear()
        }
    }

    private val browserConfiguredHandlers = mutableListOf<KotlinJsBrowserDsl.() -> Unit>()

    val browser by browserLazyDelegate

    internal val isBrowserConfigured: Boolean = browserLazyDelegate.isInitialized()

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        body(browser)
    }

    private val nodejsLazyDelegate = lazy {
        project.objects.newInstance(KotlinNodeJs::class.java, this).also {
            it.configure()
            nodejsConfiguredHandlers.forEach { handler -> handler(it) }
            nodejsConfiguredHandlers.clear()
        }
    }

    private val nodejsConfiguredHandlers = mutableListOf<KotlinJsNodeDsl.() -> Unit>()

    val nodejs by nodejsLazyDelegate

    internal val isNodejsConfigured: Boolean = nodejsLazyDelegate.isInitialized()

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        body(nodejs)
    }

    fun whenBrowserConfigured(body: KotlinJsBrowserDsl.() -> Unit) {
        if (browserLazyDelegate.isInitialized()) {
            browser(body)
        } else {
            browserConfiguredHandlers += body
        }
    }

    fun whenNodejsConfigured(body: KotlinJsNodeDsl.() -> Unit) {
        if (nodejsLazyDelegate.isInitialized()) {
            nodejs(body)
        } else {
            nodejsConfiguredHandlers += body
        }
    }

    fun useCommonJs() {
        compilations.all {
            it.compileKotlinTask.kotlinOptions {
                moduleKind = "commonjs"
                sourceMap = true
                sourceMapEmbedSources = null
            }
        }
    }
}