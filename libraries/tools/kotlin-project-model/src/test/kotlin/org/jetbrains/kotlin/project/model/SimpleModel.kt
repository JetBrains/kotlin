/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model

fun module(name: String, classifier: String? = null) = KpmBasicModule(KpmLocalModuleIdentifier("current", name, classifier))

fun KpmBasicModule.fragment(vararg nameParts: String): KpmBasicFragment =
    fragment(nameParts.drop(1).joinToString("", nameParts.first()) { it.capitalize() })

fun KpmBasicModule.fragment(name: String): KpmBasicFragment =
    fragments.firstOrNull { it.fragmentName == name } ?: KpmBasicFragment(this, name).also { fragments.add(it) }

fun KpmBasicModule.variant(vararg nameParts: String): KpmBasicVariant =
    variant(nameParts.drop(1).joinToString("", nameParts.first()) { it.capitalize() })

fun KpmBasicModule.variant(name: String): KpmBasicVariant =
    fragments.firstOrNull { it.fragmentName == name }
        ?.let { it as? KpmBasicVariant ?: error("$name is not a variant") }
        ?: KpmBasicVariant(this, name).also { fragments.add(it) }

fun KpmModuleIdentifier.equalsWithoutClassifier(other: KpmModuleIdentifier) = when (this) {
    is KpmLocalModuleIdentifier -> other is KpmLocalModuleIdentifier &&
            KpmLocalModuleIdentifier(buildId, projectId, null) == KpmLocalModuleIdentifier(other.buildId, other.projectId, null)
    is KpmMavenModuleIdentifier -> other is KpmMavenModuleIdentifier &&
            KpmMavenModuleIdentifier(group, name, null) == KpmMavenModuleIdentifier(other.group, other.name, null)
    else -> error("can't check equality yet")
}

fun KpmBasicFragment.depends(module: KpmBasicModule) {
    this.declaredModuleDependencies += KpmModuleDependency(module.moduleIdentifier)
}

fun KpmBasicFragment.refinedBy(fragment: KpmBasicFragment) {
    fragment.refines(this)
}

fun KpmBasicFragment.refines(fragment: KpmBasicFragment) {
    require(fragment.containingModule == containingModule)
    declaredRefinesDependencies.add(fragment)
}

// ---

internal data class ModuleBundle(val modules: List<KpmBasicModule>) {
    val main: KpmBasicModule
        get() = modules.single { it.moduleIdentifier.moduleClassifier == null }

    operator fun get(modulePurpose: String): KpmBasicModule = when (modulePurpose) {
        "main" -> main
        else -> modules.single { it.moduleIdentifier.moduleClassifier == modulePurpose }
    }
}

internal fun simpleModuleBundle(name: String): ModuleBundle {
    fun createModule(purpose: String): KpmBasicModule =
        module(name, purpose.takeIf { it != "main" }).apply {
            val common = fragment("common")

            val (jvm, js, linux) = listOf("jvm", "js", "linux").map { platform ->
                variant(platform).apply {
                    variantAttributes[KotlinPlatformTypeAttribute] = when (platform) {
                        "jvm" -> KotlinPlatformTypeAttribute.JVM
                        "js" -> KotlinPlatformTypeAttribute.JS
                        else -> {
                            variantAttributes[KotlinNativeTargetAttribute] = platform
                            KotlinPlatformTypeAttribute.NATIVE
                        }
                    }
                }
            }

            fragment("jvmAndJs").apply {
                refines(common)
                refinedBy(jvm)
                refinedBy(js)
            }
            fragment("jsAndLinux").apply {
                refines(common)
                refinedBy(js)
                refinedBy(linux)
            }
        }

    val main = createModule("main")
    val test = createModule("test")

    test.fragment("common").depends(main)

    return ModuleBundle(listOf(main, test))
}
