/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.util

import org.jetbrains.kotlin.codegen.topLevelClassInternalName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

fun KtClassOrObject.computeJvmInternalName(): String? {
    val name = name ?: return null
    return when (val parent = parent) {
        is KtClassBody -> (parent.parent as? KtClassOrObject)?.let { it.computeJvmInternalName() + "$" + name }
        is KtFile -> parent.packageFqName.child(Name.identifier(name)).topLevelClassInternalName()
        else -> null
    }
}
