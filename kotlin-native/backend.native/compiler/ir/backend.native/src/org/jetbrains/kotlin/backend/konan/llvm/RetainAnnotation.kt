/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName

private val retainAnnotationName = FqName("kotlin.native.Retain")
private val retainForTargetAnnotationName = FqName("kotlin.native.RetainForTarget")

internal fun IrFunction.retainAnnotation(target: KonanTarget): Boolean {
    if (this.annotations.findAnnotation(retainAnnotationName) != null) return true
    val forTarget = this.annotations.findAnnotation(retainForTargetAnnotationName)
    if (forTarget != null && forTarget.getAnnotationStringValue() == target.name) return true
    return false
}
