package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val ExportForCompilerAnnotationClassId = ClassId.topLevel(FqName("kotlin.native.internal.ExportForCompiler"))

/**
 * [kotlin.native.internal.ExportForCompiler] is an internal compiler annotation. Annotated symbols must not be exposed.
 */
internal val KtAnnotated.hasExportForCompilerAnnotation: Boolean
    get() {
        return this.hasAnnotation(ExportForCompilerAnnotationClassId)
    }