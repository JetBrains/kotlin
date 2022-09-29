/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.mpp.compilationDetailsImpl.MetadataCompilationDetails

class KotlinCommonCompilationFactory(
    override val target: KotlinOnlyTarget<*>
) : KotlinCompilationFactory<KotlinCommonCompilation> {
    override val itemClass: Class<KotlinCommonCompilation>
        get() = KotlinCommonCompilation::class.java

    override fun create(name: String): KotlinCommonCompilation = target.project.objects.newInstance(
        KotlinCommonCompilation::class.java, MetadataCompilationDetails(target, name, getOrCreateDefaultSourceSet(name))
    )
}
