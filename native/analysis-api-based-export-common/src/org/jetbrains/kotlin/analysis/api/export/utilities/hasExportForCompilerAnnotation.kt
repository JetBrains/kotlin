/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val ExportForCompilerAnnotationClassId = ClassId.topLevel(FqName("kotlin.native.internal.ExportForCompiler"))

/**
 * [kotlin.native.internal.ExportForCompiler] is an internal compiler annotation. Annotated symbols must not be exposed.
 */
public val KaAnnotated.hasExportForCompilerAnnotation: Boolean
    get() {
        return ExportForCompilerAnnotationClassId in annotations
    }