/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation


interface GradleKpmConfigurationRelationSetup {
    fun setupExtendsFromRelations(configuration: Configuration, context: GradleKpmFragmentConfigureContext)

    object None : GradleKpmConfigurationRelationSetup {
        override fun setupExtendsFromRelations(configuration: Configuration, context: GradleKpmFragmentConfigureContext) = Unit
    }
}

operator fun GradleKpmConfigurationRelationSetup.plus(other: GradleKpmConfigurationRelationSetup): GradleKpmConfigurationRelationSetup {
    if (this === GradleKpmConfigurationRelationSetup.None) return other
    if (other === GradleKpmConfigurationRelationSetup.None) return this

    if (this is CompositeKpmConfigurationRelationSetup && other is CompositeKpmConfigurationRelationSetup) {
        return CompositeKpmConfigurationRelationSetup(this.children + other.children)
    }

    if (this is CompositeKpmConfigurationRelationSetup) {
        return CompositeKpmConfigurationRelationSetup(this.children + other)
    }

    if (other is CompositeKpmConfigurationRelationSetup) {
        return CompositeKpmConfigurationRelationSetup(listOf(this) + other.children)
    }

    return CompositeKpmConfigurationRelationSetup(listOf(this, other))
}

internal class CompositeKpmConfigurationRelationSetup(val children: List<GradleKpmConfigurationRelationSetup>) :
    GradleKpmConfigurationRelationSetup {

    override fun setupExtendsFromRelations(configuration: Configuration, context: GradleKpmFragmentConfigureContext) {
        children.forEach { child -> child.setupExtendsFromRelations(configuration, context) }
    }
}

class GradleKpmFragmentConfigureRelationContext internal constructor(
    val configuration: Configuration,
    context: GradleKpmFragmentConfigureContext
) : GradleKpmFragmentConfigureContext by context {
    fun extendsFrom(configuration: Configuration) = this.configuration.extendsFrom(configuration)
    fun extendsFrom(configuration: String) = project.addExtendsFromRelation(this.configuration.name, configuration)
}

fun GradleKpmConfigurationRelationSetup(
    setExtendsFrom: GradleKpmFragmentConfigureRelationContext.() -> Unit
): GradleKpmConfigurationRelationSetup = object : GradleKpmConfigurationRelationSetup {
    override fun setupExtendsFromRelations(configuration: Configuration, context: GradleKpmFragmentConfigureContext) {
        val relationContext = GradleKpmFragmentConfigureRelationContext(configuration, context)
        relationContext.setExtendsFrom()
    }
}
