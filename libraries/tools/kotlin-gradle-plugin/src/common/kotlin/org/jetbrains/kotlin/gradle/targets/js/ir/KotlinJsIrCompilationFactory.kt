/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.mpp.JsIrCompilationDetails
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.getOrCreateDefaultSourceSet
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class KotlinJsIrCompilationFactory(
    override val target: KotlinJsIrTarget
) : KotlinCompilationFactory<KotlinJsIrCompilation> {
    override val itemClass: Class<KotlinJsIrCompilation>
        get() = KotlinJsIrCompilation::class.java

    override fun defaultSourceSetName(compilationName: String): String {
        return lowerCamelCaseName(
            if (target.mixedMode)
                target.disambiguationClassifierInPlatform
            else
                target.disambiguationClassifier,
            compilationName
        )
    }

    override fun create(name: String): KotlinJsIrCompilation =
        target.project.objects.newInstance(
            KotlinJsIrCompilation::class.java, JsIrCompilationDetails(target, name, getOrCreateDefaultSourceSet(name))
        )
}