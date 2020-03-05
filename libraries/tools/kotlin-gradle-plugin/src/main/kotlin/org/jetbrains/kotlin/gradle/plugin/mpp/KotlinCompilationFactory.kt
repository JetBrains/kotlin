/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectFactory
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

interface KotlinCompilationFactory<T : KotlinCompilation<*>> : NamedDomainObjectFactory<T> {
    val itemClass: Class<T>
}

class KotlinCommonCompilationFactory(
    val target: KotlinOnlyTarget<*>
) : KotlinCompilationFactory<KotlinCommonCompilation> {
    override val itemClass: Class<KotlinCommonCompilation>
        get() = KotlinCommonCompilation::class.java

    override fun create(name: String): KotlinCommonCompilation =
        KotlinCommonCompilation(target, name)
}