/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.compiler.resolve;

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object StandardIds {
    val KOTLIN_JS_FQN = FqName("kotlin.js")
    val JS_FUNCTION_ID = CallableId(KOTLIN_JS_FQN, Name.identifier("js"))
}

object JsObjectAnnotations {
    val jsSimpleObjectAnnotationClassId = ClassId(FqName("kotlinx.jso"), Name.identifier("JsSimpleObject"))
    val jsSimpleObjectAnnotationFqName = jsSimpleObjectAnnotationClassId.asSingleFqName()
}