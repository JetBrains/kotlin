/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.plugin.sources.android.kotlinAndroidSourceSetLayout

class KotlinJvmAndroidCompilationFactory(
    override val target: KotlinAndroidTarget,
    private val variant: BaseVariant,
) : KotlinCompilationFactory<KotlinJvmAndroidCompilation> {

    override val itemClass: Class<KotlinJvmAndroidCompilation>
        get() = KotlinJvmAndroidCompilation::class.java


    override fun defaultSourceSetName(compilationName: String): String {
        return project.kotlinAndroidSourceSetLayout.naming.defaultKotlinSourceSetName(target, variant)
            ?: super.defaultSourceSetName(compilationName)
    }

    override fun create(name: String): KotlinJvmAndroidCompilation {
        lateinit var result: KotlinJvmAndroidCompilation
        val details = AndroidCompilationDetails(target, name, getOrCreateDefaultSourceSet(name), variant) { result }
        result = project.objects.newInstance(KotlinJvmAndroidCompilation::class.java, details)
        return result
    }

}