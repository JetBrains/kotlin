/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.PRESETS_API_IS_DEPRECATED_MESSAGE
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptionsDeprecated
import org.jetbrains.kotlin.gradle.dsl.KotlinGradlePluginDsl
import org.jetbrains.kotlin.tooling.core.HasMutableExtras

@KotlinGradlePluginDsl
interface KotlinTarget : Named, HasAttributes, HasProject, HasMutableExtras {
    val targetName: String
    val disambiguationClassifier: String? get() = targetName

    /* Long deprecation cycle, because IDE might be calling into this via reflection */
    @Deprecated("Scheduled for removal with Kotlin 2.2")
    val useDisambiguationClassifierAsSourceSetNamePrefix: Boolean

    /* Long deprecation cycle, because IDE might be calling into this via reflection */
    @Deprecated("Scheduled for removal with Kotlin 2.2")
    val overrideDisambiguationClassifierOnIdeImport: String?

    val platformType: KotlinPlatformType

    val compilations: NamedDomainObjectContainer<out KotlinCompilation<KotlinCommonOptionsDeprecated>>

    val artifactsTaskName: String

    val apiElementsConfigurationName: String
    val runtimeElementsConfigurationName: String
    val sourcesElementsConfigurationName: String

    val publishable: Boolean

    fun withSourcesJar(publish: Boolean = true)

    val components: Set<SoftwareComponent>

    fun mavenPublication(action: MavenPublication.() -> Unit) = mavenPublication(Action { action(it) })
    fun mavenPublication(action: Action<MavenPublication>)

    fun attributes(configure: AttributeContainer.() -> Unit) = attributes.configure()
    fun attributes(configure: Action<AttributeContainer>) = attributes { configure.execute(this) }

    @OptIn(DeprecatedTargetPresetApi::class, InternalKotlinGradlePluginApi::class)
    @Deprecated(
        PRESETS_API_IS_DEPRECATED_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    val preset: KotlinTargetPreset<out KotlinTarget>?

    override fun getName(): String = targetName

    @Deprecated(
        "Accessing 'sourceSets' container on the Kotlin target level DSL is deprecated. " +
                "Consider configuring 'sourceSets' on the Kotlin extension level.",
        level = DeprecationLevel.WARNING
    )
    val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>
}

interface KotlinTargetWithTests<E : KotlinExecution.ExecutionSource, T : KotlinTargetTestRun<E>> : KotlinTarget {
    /** The container with the test run executions.
     * A target may automatically create and configure a test run by the name [DEFAULT_TEST_RUN_NAME]. */
    val testRuns: NamedDomainObjectContainer<T>

    companion object {
        const val DEFAULT_TEST_RUN_NAME = "test"
    }
}
