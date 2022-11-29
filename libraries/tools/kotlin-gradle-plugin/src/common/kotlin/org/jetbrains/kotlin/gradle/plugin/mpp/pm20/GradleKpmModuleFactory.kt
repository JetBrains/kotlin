/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.pm20Extension

open class GradleKpmModuleFactory(private val project: Project) : NamedDomainObjectFactory<GradleKpmModule> {
    override fun create(name: String): GradleKpmModule {
        val result = project.objects.newInstance(GradleKpmModuleInternal::class.java, project, name)
        registerFragmentFactory(result)
        registerDefaultCommonFragment(result)
        addDefaultDependencyOnMainModule(result)
        addDefaultRefinementDependencyOnCommon(result)
        return result
    }

    protected open fun registerDefaultCommonFragment(module: GradleKpmModule) {
        module.fragments.register(GradleKpmFragment.COMMON_FRAGMENT_NAME, GradleKpmFragment::class.java)
    }

    protected open fun addDefaultRefinementDependencyOnCommon(module: GradleKpmModule) {
        module.fragments.matching { it.name != GradleKpmFragment.COMMON_FRAGMENT_NAME }.all {
            it.refines(module.fragments.named(GradleKpmFragment.COMMON_FRAGMENT_NAME))
        }
    }

    protected open fun addDefaultDependencyOnMainModule(module: GradleKpmModule) {
        if (module.name != GradleKpmModule.MAIN_MODULE_NAME) {
            module.fragments
                .matching { it.fragmentName == GradleKpmFragment.COMMON_FRAGMENT_NAME }
                .configureEach { commonFragment ->
                    commonFragment.dependencies {
                        api(module.project.pm20Extension.modules.getByName(GradleKpmModule.MAIN_MODULE_NAME))
                    }
                }
        }
    }

    protected open fun registerFragmentFactory(module: GradleKpmModule) {
        module.fragments.registerFactory(GradleKpmFragment::class.java, GradleKpmCommonFragmentFactory(module))
    }
}
