/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface KotlinEntity

interface KotlinFunction : KotlinEntity {
    val fqName: List<String>
    val parameters: List<KotlinParameter>
    val returnType: KotlinType
}

interface KotlinParameter : KotlinEntity {
    val name: String
    val type: KotlinType
}

interface KotlinType : KotlinEntity {
    val name: String
}
