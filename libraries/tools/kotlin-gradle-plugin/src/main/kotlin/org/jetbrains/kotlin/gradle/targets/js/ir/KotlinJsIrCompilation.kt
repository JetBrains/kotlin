/*
* Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
*/

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class KotlinJsIrCompilation(
    target: KotlinTarget,
    name: String
) : KotlinJsCompilation(target, name) {
    val productionCompileTaskName: String = lowerCamelCaseName(
        "compile",
        "production",
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        "Kotlin",
        target.targetName
    )

    val productionCompileTask: Kotlin2JsCompile =
        (target.project.tasks.getByName(productionCompileTaskName) as Kotlin2JsCompile)

    val developmentCompileTaskName: String = lowerCamelCaseName(
        "compile",
        "development",
        compilationName.takeIf { it != KotlinCompilation.MAIN_COMPILATION_NAME },
        "Kotlin",
        target.targetName
    )

    val developmentCompileTask: Kotlin2JsCompile =
        (target.project.tasks.getByName(developmentCompileTaskName) as Kotlin2JsCompile)
}