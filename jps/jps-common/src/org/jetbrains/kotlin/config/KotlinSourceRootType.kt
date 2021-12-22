/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

sealed class KotlinSourceRootType() : JpsElementTypeBase<JavaSourceRootProperties>(), JpsModuleSourceRootType<JavaSourceRootProperties> {

    override fun createDefaultProperties() = JpsJavaExtensionService.getInstance().createSourceRootProperties("")

}

object SourceKotlinRootType : KotlinSourceRootType()

object TestSourceKotlinRootType : KotlinSourceRootType() {
    override fun isForTests() = true
}

val ALL_KOTLIN_SOURCE_ROOT_TYPES = setOf(SourceKotlinRootType, TestSourceKotlinRootType)
