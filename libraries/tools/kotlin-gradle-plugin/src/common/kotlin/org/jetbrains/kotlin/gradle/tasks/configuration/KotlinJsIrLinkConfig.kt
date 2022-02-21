/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink

internal open class KotlinJsIrLinkConfig(
    compilation: KotlinJsIrCompilation
) : BaseKotlin2JsCompileConfig<KotlinJsIrLink>(compilation) {

    init {
        configureTask { task ->
            // Link tasks are not affected by compiler plugin, so set to empty
            task.pluginClasspath.setFrom(objectFactory.fileCollection())

            task.entryModule.fileProvider(compilation.output.classesDirs.elements.map { it.single().asFile }).disallowChanges()
            task.compilation = compilation
            task.destinationDirectory.fileProvider(task.outputFileProperty.map { it.parentFile })
        }
    }
}