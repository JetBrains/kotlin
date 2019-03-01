/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.mpp

class KotlinJvmCompilationFactory(
    val target: KotlinOnlyTarget<KotlinJvmCompilation>
) : KotlinCompilationFactory<KotlinJvmCompilation> {
    override val itemClass: Class<KotlinJvmCompilation>
        get() = KotlinJvmCompilation::class.java

    override fun create(name: String): KotlinJvmCompilation =
        KotlinJvmCompilation(target, name)
}