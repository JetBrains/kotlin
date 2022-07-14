/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.testDsl

import org.jetbrains.kotlin.project.model.KotlinPlatformTypeAttribute
import org.jetbrains.kotlin.project.model.infra.TestKpmFragment
import org.jetbrains.kotlin.project.model.infra.TestKpmModuleContainer
import org.jetbrains.kotlin.project.model.infra.TestKpmModule
import org.jetbrains.kotlin.project.model.infra.TestKpmVariant

fun TestKpmModule.fragment(name: String, applyDefaults: Boolean = true, configure: TestKpmFragment.() -> Unit = { }): TestKpmFragment {
    val result = fragments.getOrPut(name) {
        val fragment = TestKpmFragment(this, name)
        fragment
    }
    if (applyDefaults) result.applyDefaults()
    return result.also(configure)
}

fun TestKpmModule.fragmentNamed(name: String): TestKpmFragment =
    fragments[name] ?: error("Fragment with name $name doesn't exist. Existing fragments ${fragments.joinToString { it.name }}")

fun TestKpmModule.variant(
    name: String,
    platform: String, // from KotlinPlatformTypeAttribute
    applyDefaults: Boolean = true,
    configure: TestKpmVariant.() -> Unit = { }
): TestKpmVariant {
    val result = fragments.getOrPut(name) {
        val variant = TestKpmVariant(this, name)
        variant.variantAttributes[KotlinPlatformTypeAttribute] = platform
        variant
    } as TestKpmVariant
    if (applyDefaults) result.applyDefaults()
    result.configure()
    return result
}

fun TestKpmModule.depends(otherModule: TestKpmModule): TestKpmModule {
    common.depends(otherModule)
    return this
}

fun TestKpmModule.depends(otherProject: TestKpmModuleContainer): TestKpmModule {
    common.depends(otherProject.main)
    return this
}

val TestKpmModule.common: TestKpmFragment get() = fragmentNamed("common")
