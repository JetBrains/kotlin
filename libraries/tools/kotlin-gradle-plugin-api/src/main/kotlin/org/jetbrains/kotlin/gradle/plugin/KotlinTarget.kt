/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions

interface KotlinTargetComponent : SoftwareComponent {
    val target: KotlinTarget
    val publishable: Boolean
    val defaultArtifactId: String
    val sourcesArtifacts: Set<PublishArtifact>
}

interface KotlinTarget : Named, HasAttributes {
    val targetName: String
    val disambiguationClassifier: String? get() = targetName

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

    fun attributes(configure: AttributeContainer.() -> Unit) = configure(attributes)
    fun attributes(configure: Closure<*>) = attributes { ConfigureUtil.configure(configure, this) }

    val preset: KotlinTargetPreset<out KotlinTarget>?

    override fun getName(): String = targetName
}