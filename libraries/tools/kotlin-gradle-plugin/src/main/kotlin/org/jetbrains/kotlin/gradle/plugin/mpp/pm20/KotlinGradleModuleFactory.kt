/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.pm20Extension

open class KotlinGradleModuleFactory(private val project: Project) : NamedDomainObjectFactory<KotlinGradleModule> {
    override fun create(name: String): KotlinGradleModule {
        val result = project.objects.newInstance(KotlinGradleModuleInternal::class.java, project, name)
        registerFragmentFactory(result)
        registerDefaultCommonFragment(result)
        addDefaultDependencyOnMainModule(result)
        addDefaultRefinementDependencyOnCommon(result)
        return result
    }

    protected open fun registerDefaultCommonFragment(module: KotlinGradleModule) {
        module.fragments.register(KotlinGradleFragment.COMMON_FRAGMENT_NAME, KotlinGradleFragment::class.java)
    }

    protected open fun addDefaultRefinementDependencyOnCommon(module: KotlinGradleModule) {
        module.fragments.matching { it.name != KotlinGradleFragment.COMMON_FRAGMENT_NAME }.all {
            it.refines(module.fragments.named(KotlinGradleFragment.COMMON_FRAGMENT_NAME))
        }
    }

    protected open fun addDefaultDependencyOnMainModule(module: KotlinGradleModule) {
        if (module.name != KotlinGradleModule.MAIN_MODULE_NAME) {
            module.fragments
                .matching { it.fragmentName == KotlinGradleFragment.COMMON_FRAGMENT_NAME }
                .configureEach { commonFragment ->
                    commonFragment.dependencies {
                        api(module.project.pm20Extension.modules.getByName(KotlinGradleModule.MAIN_MODULE_NAME))
                    }
                }
        }
    }

    protected open fun registerFragmentFactory(module: KotlinGradleModule) {
        module.fragments.registerFactory(KotlinGradleFragment::class.java, CommonGradleFragmentFactory(module))
    }
}