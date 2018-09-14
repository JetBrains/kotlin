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
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication

interface KotlinTargetComponent : SoftwareComponent {
    val target: KotlinTarget
    val publishable: Boolean
}

interface KotlinTarget: Named, HasAttributes {
    val targetName: String
    val disambiguationClassifier: String? get() = targetName

    val platformType: KotlinPlatformType

    val compilations: NamedDomainObjectContainer<out KotlinCompilation>

    val project: Project

    val artifactsTaskName: String

    val defaultConfigurationName: String
    val apiElementsConfigurationName: String
    val runtimeElementsConfigurationName: String

    val publishable: Boolean

    val component: KotlinTargetComponent

    fun createUsageContexts(): Set<UsageContext>

    fun publication(action: Closure<Unit>)
    fun publication(action: Action<MavenPublication>)

    override fun getName(): String = targetName
}