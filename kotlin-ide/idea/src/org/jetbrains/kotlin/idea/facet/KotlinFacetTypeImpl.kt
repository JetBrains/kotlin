/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.Facet
import com.intellij.facet.ui.FacetEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class KotlinFacetTypeImpl : KotlinFacetType<KotlinFacetConfiguration>() {

    override fun createDefaultConfiguration() = KotlinFacetConfigurationImpl()

    override fun createFacet(
        module: Module,
        name: String,
        configuration: KotlinFacetConfiguration,
        underlyingFacet: Facet<*>?
    ) = KotlinFacet(module, name, configuration)

    override fun createMultipleConfigurationsEditor(project: Project, editors: Array<out FacetEditor>) =
        MultipleKotlinFacetEditor(project, editors)
}