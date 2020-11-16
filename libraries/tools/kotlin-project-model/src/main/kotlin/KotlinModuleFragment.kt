/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import java.io.File

interface KotlinModuleFragment {
    val containingModule: KotlinModule

    val fragmentName: String
    val directRefinesDependencies: Iterable<KotlinModuleFragment>

    val declaredContainingModuleFragmentDependencies: Iterable<KotlinModuleFragment>

    // TODO: scopes
    val declaredModuleDependencies: Iterable<ModuleDependency>

    val kotlinSourceRoots: Iterable<File>
}

interface KotlinModuleVariant : KotlinModuleFragment {
    val variantAttributes: Map<KotlinAttributeKey, String>

    var isExported: Boolean
}

val KotlinModuleFragment.fragmentAttributeSets: Map<KotlinAttributeKey, Set<String>>
    get() = mutableMapOf<KotlinAttributeKey, MutableSet<String>>().apply {
        containingModule.variantsContainingFragment(this@fragmentAttributeSets).forEach { variant ->
            variant.variantAttributes.forEach { (attribute, value) ->
                getOrPut(attribute) { mutableSetOf() }.add(value)
            }
        }
    }

val KotlinModuleFragment.refinesClosure: Set<KotlinModuleFragment>
    get() = mutableSetOf<KotlinModuleFragment>().apply {
        fun visit(moduleFragment: KotlinModuleFragment) {
            if (add(moduleFragment))
                moduleFragment.directRefinesDependencies.forEach(::visit)
        }
        visit(this@refinesClosure)
    }


open class BasicKotlinModuleFragment(
    override val containingModule: KotlinModule,
    override val fragmentName: String
) : KotlinModuleFragment {

    override val directRefinesDependencies: MutableSet<BasicKotlinModuleFragment> = mutableSetOf()

    override val declaredContainingModuleFragmentDependencies: MutableSet<BasicKotlinModuleFragment> = mutableSetOf()
    override val declaredModuleDependencies: MutableSet<ModuleDependency> = mutableSetOf()

    override var kotlinSourceRoots: Iterable<File> = emptyList()
    override fun toString(): String = "fragment $fragmentName"
}

class BasicKotlinModuleVariant(
    containingModule: KotlinModule,
    fragmentName: String
) : BasicKotlinModuleFragment (
    containingModule,
    fragmentName
), KotlinModuleVariant {
    override var isExported: Boolean = true
    override val variantAttributes: MutableMap<KotlinAttributeKey, String> = mutableMapOf()
    override fun toString(): String = "variant $fragmentName"
}