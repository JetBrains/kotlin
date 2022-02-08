/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

internal class FragmentGranularMetadataResolverFactory {
    private val resolvers = mutableMapOf<KotlinGradleFragment, FragmentGranularMetadataResolver>()

    fun getOrCreate(fragment: KotlinGradleFragment): FragmentGranularMetadataResolver = resolvers.getOrPut(fragment) {
        FragmentGranularMetadataResolver(fragment, lazy {
            fragment.refinesClosure.minus(fragment).map { refinesFragment ->
                getOrCreate(refinesFragment)
            }
        })
    }
}
