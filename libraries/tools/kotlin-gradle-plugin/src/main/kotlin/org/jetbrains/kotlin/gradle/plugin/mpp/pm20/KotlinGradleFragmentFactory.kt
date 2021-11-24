/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.NamedDomainObjectFactory

class KotlinGradleFragmentFactory<T : KotlinGradleFragment>(
    private val fragmentInstantiator: FragmentInstantiator<T>,
    private val fragmentConfigurator: FragmentConfigurator<T>
) : NamedDomainObjectFactory<T> {

    interface FragmentInstantiator<out T : KotlinGradleFragment> {
        fun create(name: String): T
    }

    interface FragmentConfigurator<in T : KotlinGradleFragment> {
        fun configure(fragment: T)
    }

    override fun create(name: String): T {
        return fragmentInstantiator.create(name).apply(fragmentConfigurator::configure)
    }
}
