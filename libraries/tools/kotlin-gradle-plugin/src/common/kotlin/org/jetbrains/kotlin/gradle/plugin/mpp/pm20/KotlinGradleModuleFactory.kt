/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

open class KotlinGradleModuleFactory(private val project: Project) : NamedDomainObjectFactory<KpmGradleModule> {
    override fun create(name: String): KpmGradleModule {
        val result = project.objects.newInstance(KpmGradleModuleInternal::class.java, project, name)
        registerFragmentFactory(result)
        registerDefaultCommonFragment(result)
        addDefaultDependencyOnMainModule(result)
        addDefaultRefinementDependencyOnCommon(result)
        return result
    }

    protected open fun registerDefaultCommonFragment(module: KpmGradleModule) {
        module.fragments.register(KpmGradleFragment.COMMON_FRAGMENT_NAME, KpmGradleFragment::class.java)
    }

    protected open fun addDefaultRefinementDependencyOnCommon(module: KpmGradleModule) {
        module.fragments.matching { it.name != KpmGradleFragment.COMMON_FRAGMENT_NAME }.all {
            it.refines(module.fragments.named(KpmGradleFragment.COMMON_FRAGMENT_NAME))
        }
    }

    protected open fun addDefaultDependencyOnMainModule(module: KpmGradleModule) {
        if (module.name != KpmGradleModule.MAIN_MODULE_NAME) {
            module.fragments
                .matching { it.fragmentName == KpmGradleFragment.COMMON_FRAGMENT_NAME }
                .configureEach { commonFragment ->
                    commonFragment.dependencies {
                        api(module.project.kpmModules.getByName(KpmGradleModule.MAIN_MODULE_NAME))
                    }
                }
        }
    }

    protected open fun registerFragmentFactory(module: KpmGradleModule) {
        module.fragments.registerFactory(KpmGradleFragment::class.java, KotlinCommonFragmentFactory(module))
    }
}
