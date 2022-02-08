/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions

interface KotlinTargetComponent : SoftwareComponent {
    val target: KotlinTarget
    val publishable: Boolean
    val publishableOnCurrentHost: Boolean
    val defaultArtifactId: String
    val sourcesArtifacts: Set<PublishArtifact>
}

interface KotlinTarget : Named, HasAttributes {
    val targetName: String
    val disambiguationClassifier: String? get() = targetName
    val useDisambiguationClassifierAsSourceSetNamePrefix: Boolean
    val overrideDisambiguationClassifierOnIdeImport: String?

    val platformType: KotlinPlatformType

    val compilations: NamedDomainObjectContainer<out KotlinCompilation<KotlinCommonOptions>>

    val project: Project

    val artifactsTaskName: String

    val defaultConfigurationName: String
    val apiElementsConfigurationName: String
    val runtimeElementsConfigurationName: String

    val publishable: Boolean

    val components: Set<SoftwareComponent>

    fun mavenPublication(action: Closure<Unit>)
    fun mavenPublication(action: Action<MavenPublication>)

    fun attributes(configure: AttributeContainer.() -> Unit) = attributes.configure()
    fun attributes(configure: Closure<*>) = attributes { project.configure(this, configure) }

    val preset: KotlinTargetPreset<out KotlinTarget>?

    override fun getName(): String = targetName
}

interface KotlinTargetWithTests<E : KotlinExecution.ExecutionSource, T : KotlinTargetTestRun<E>> : KotlinTarget {
    /** The container with the test run executions.
     * A target may automatically create and configure a test run by the name [DEFAULT_TEST_RUN_NAME]. */
    val testRuns: NamedDomainObjectContainer<T>

    companion object {
        const val DEFAULT_TEST_RUN_NAME = "test"
    }
}
