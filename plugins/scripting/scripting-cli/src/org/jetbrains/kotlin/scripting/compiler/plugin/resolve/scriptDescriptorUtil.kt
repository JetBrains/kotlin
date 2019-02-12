/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.resolve

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass
import kotlin.reflect.KType

val KClass<*>.classId: ClassId
    get() = this.java.enclosingClass?.kotlin?.classId?.createNestedClassId(Name.identifier(simpleName!!))
            ?: ClassId.topLevel(FqName(qualifiedName!!))

val KType.classId: ClassId?
    get() = classifier?.let { it as? KClass<*> }?.classId
