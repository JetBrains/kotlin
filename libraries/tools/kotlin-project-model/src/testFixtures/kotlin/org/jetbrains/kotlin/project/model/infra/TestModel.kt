/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.*
import org.jetbrains.kotlin.project.model.testDsl.*
import org.jetbrains.kotlin.project.model.utils.ObservableIndexedCollection
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import java.io.File

interface KpmTestEntity {
    val name: String
}

class KpmTestCase(
    override val name: String,
) : KpmTestEntity {
    val projects: ObservableIndexedCollection<TestKpmModuleContainer> = ObservableIndexedCollection()
    val extras: MutableExtras = mutableExtrasOf()

    override fun toString(): String = "Case $name"
}

class TestKpmModuleContainer(
    val containingCase: KpmTestCase,
    override val name: String,
) : KpmTestEntity {
    val modules: ObservableIndexedCollection<TestKpmModule> = ObservableIndexedCollection()
    val extras: MutableExtras = mutableExtrasOf()

    fun applyDefaults() {
        module("main")
    }

    override fun toString(): String = ":$name"
}

class TestKpmModule(
    val containingProject: TestKpmModuleContainer,
    override val moduleIdentifier: KpmModuleIdentifier,
) : KpmTestEntity, KpmModule {
    override val fragments: ObservableIndexedCollection<TestKpmFragment> = ObservableIndexedCollection()
    override val plugins: MutableSet<KpmCompilerPlugin> = mutableSetOf()
    val extras: MutableExtras = mutableExtrasOf()

    override val name: String
        get() = moduleIdentifier.moduleClassifier ?: "main"

    fun applyDefaults() {
        fragment("common")
    }
}

open class TestKpmFragment(
    override val containingModule: TestKpmModule,
    override val fragmentName: String,
) : KpmTestEntity, KpmFragment {
    override var languageSettings: LanguageSettings? = null
    val extras: MutableExtras = mutableExtrasOf()
    override val kotlinSourceRoots: MutableList<File> = mutableListOf()
    override val declaredModuleDependencies: MutableList<KpmModuleDependency> = mutableListOf()
    override val declaredRefinesDependencies: MutableList<TestKpmFragment> = mutableListOf()
    override val name: String get() = fragmentName

    fun applyDefaults() {
        refines(containingModule.common)
    }
}

class TestKpmVariant(
    containingModule: TestKpmModule,
    fragmentName: String,
) : TestKpmFragment(containingModule, fragmentName), KpmTestEntity, KpmVariant {
    override val variantAttributes: MutableMap<KotlinAttributeKey, String> = mutableMapOf()
}
