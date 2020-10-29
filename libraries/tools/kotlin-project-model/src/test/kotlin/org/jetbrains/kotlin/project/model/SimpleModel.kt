/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

fun module(name: String) = BasicKotlinModule(name, ModuleSource.LocalBuild("current"))

fun BasicKotlinModule.fragment(vararg nameParts: String): BasicKotlinModuleFragment =
    fragment(nameParts.drop(1).joinToString("", nameParts.first()) { it.capitalize() })

fun BasicKotlinModule.fragment(name: String): BasicKotlinModuleFragment =
    fragments.firstOrNull { it.fragmentName == name } ?: BasicKotlinModuleFragment(this, name).also { fragments.add(it) }

fun BasicKotlinModule.variant(vararg nameParts: String): BasicKotlinVariant =
    variant(nameParts.drop(1).joinToString("", nameParts.first()) { it.capitalize() })

fun BasicKotlinModule.variant(name: String): BasicKotlinVariant =
    fragments.firstOrNull { it.fragmentName == name }
        ?.let { it as? BasicKotlinVariant ?: error("$name is not a variant") }
        ?: BasicKotlinVariant(this, name).also { fragments.add(it) }


fun BasicKotlinModuleFragment.depends(fragment: BasicKotlinModuleFragment) {
    require(fragment.containingModule == containingModule)
    declaredContainingModuleFragmentDependencies.add(fragment)
}

fun BasicKotlinModuleFragment.refinedBy(fragment: BasicKotlinModuleFragment) {
    fragment.refines(this)
}

fun BasicKotlinModuleFragment.refines(fragment: BasicKotlinModuleFragment) {
    require(fragment.containingModule == containingModule)
    directRefinesDependencies.add(fragment)
}

// ---

fun simpleModule(name: String) = module(name).apply {
    listOf("main", "test").forEach { purpose ->
        val common = fragment("common", purpose)
        if (purpose == "test")
            common.depends(fragment("common", "main"))

        val (jvm, js, linux) = listOf("jvm", "js", "linux").map { platform ->
            variant(platform, purpose).apply {
                variantAttributes[KotlinPlatformTypeAttribute] = when (platform) {
                    "jvm" -> KotlinPlatformTypeAttribute.JVM
                    "js" -> KotlinPlatformTypeAttribute.JS
                    else -> {
                        variantAttributes[KotlinNativeTargetAttribute] = platform
                        KotlinPlatformTypeAttribute.NATIVE
                    }
                }
                if (purpose == "test") {
                    isExported = false
                    depends(variant(platform, "main"))
                }
            }
        }

        val jvmAndJs = fragment("jvmAndJs", purpose).apply {
            refines(common)
            refinedBy(jvm)
            refinedBy(js)
        }
        val jsAndLinux = fragment("jsAndLinux", purpose).apply {
            refines(common)
            refinedBy(js)
            refinedBy(linux)
        }
    }
}