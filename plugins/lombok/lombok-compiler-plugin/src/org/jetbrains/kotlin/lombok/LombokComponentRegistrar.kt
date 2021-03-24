/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.SyntheticJavaResolveExtension

class LombokComponentRegistrar : ComponentRegistrar {

    companion object {
        fun registerComponents(project: Project) {
            SyntheticJavaResolveExtension.registerExtension(project, LombokResolveExtension())
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerComponents(project)
    }
}
