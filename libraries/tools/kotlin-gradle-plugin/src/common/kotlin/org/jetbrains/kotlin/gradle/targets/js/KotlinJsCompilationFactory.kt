/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class KotlinJsCompilationFactory(
    override val target: KotlinOnlyTarget<KotlinJsCompilation>,
) : KotlinCompilationFactory<KotlinJsCompilation> {
    override val itemClass: Class<KotlinJsCompilation>
        get() = KotlinJsCompilation::class.java

    override fun defaultSourceSetName(compilationName: String): String {
        val classifier = if (target is KotlinJsTarget && target.irTarget != null)
            target.disambiguationClassifierInPlatform
        else target.disambiguationClassifier

        return lowerCamelCaseName(
            classifier,
            compilationName
        )
    }

    override fun create(name: String): KotlinJsCompilation = target.project.objects.newInstance(
        KotlinJsCompilation::class.java, JsCompilationDetails(target, name, getOrCreateDefaultSourceSet(name))
    )
}