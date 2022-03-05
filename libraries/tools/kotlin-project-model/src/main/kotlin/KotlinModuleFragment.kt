/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment
import org.jetbrains.kotlin.tooling.core.closure
import org.jetbrains.kotlin.tooling.core.withClosure
import java.io.File

interface KotlinModuleFragment {
    val containingModule: KotlinModule

    val fragmentName: String
    val directRefinesDependencies: Iterable<KotlinModuleFragment>

    val languageSettings: LanguageSettings?

    // TODO: scopes
    val declaredModuleDependencies: Iterable<KotlinModuleDependency>

    // TODO: should this be source roots or source files?
    val kotlinSourceRoots: Iterable<File>

    companion object
}

interface KotlinModuleVariant : KotlinModuleFragment {
    val variantAttributes: Map<KotlinAttributeKey, String>
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
    get() = this.closure { it.directRefinesDependencies }

val KotlinModuleFragment.withRefinesClosure: Set<KotlinModuleFragment>
    get() = this.withClosure { it.directRefinesDependencies }

val KotlinModuleVariant.platform get() = variantAttributes[KotlinPlatformTypeAttribute]

open class BasicKotlinModuleFragment(
    override val containingModule: KotlinModule,
    override val fragmentName: String,
    override val languageSettings: LanguageSettings? = null
) : KotlinModuleFragment {

    override val directRefinesDependencies: MutableSet<BasicKotlinModuleFragment> = mutableSetOf()

    override val declaredModuleDependencies: MutableSet<KotlinModuleDependency> = mutableSetOf()

    override var kotlinSourceRoots: Iterable<File> = emptyList()
    override fun toString(): String = "fragment $fragmentName"
}

class BasicKotlinModuleVariant(
    containingModule: KotlinModule,
    fragmentName: String,
    languageSettings: LanguageSettings? = null
) : BasicKotlinModuleFragment(
    containingModule,
    fragmentName,
    languageSettings
), KotlinModuleVariant {
    override val variantAttributes: MutableMap<KotlinAttributeKey, String> = mutableMapOf()
    override fun toString(): String = "variant $fragmentName"
}
