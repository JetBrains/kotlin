/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue.KtStringConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames

/**
 * Represents the values resolved from the [kotlin.native.ObjCName] annotation.
 *
 * ### Example
 *
 * **Given a class Foo**
 * ```kotlin
 * @ObjCName("FooObjC", "FooSwift", true)
 * class Foo
 * ```
 *
 * **Given class Foo being analyzed**
 * ```kotlin
 * val foo = getFooClassOrObjectSymbol()
 *            //   ^
 *            //   Imaginary method to get the symbol 'Foo' from above
 *
 * val resolvedObjCNameAnnotation = foo.resolveObjCNameAnnotation()
 * //       ^
 * // objCName = "FooObjC"
 * // swiftName = "FooSwift"
 * // isExaclt = true
 * ```
 */
internal class KtResolvedObjCNameAnnotation(
    val objCName: String?,
    val swiftName: String?,
    val isExact: Boolean,
)

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.resolveObjCNameAnnotation(): KtResolvedObjCNameAnnotation? {
    val annotation = annotationsList.annotations.find { it.classId?.asSingleFqName() == KonanFqNames.objCName } ?: return null

    return KtResolvedObjCNameAnnotation(
        objCName = annotation.findArgument("name")?.resolveStringConstantValue(),
        swiftName = annotation.findArgument("swiftName")?.resolveStringConstantValue(),
        isExact = annotation.findArgument("exact")?.resolveBooleanConstantValue() ?: false
    )
}

private fun KtAnnotationApplicationWithArgumentsInfo.findArgument(name: String): KtNamedAnnotationValue? {
    return arguments.find { it.name.identifier == name }
}

private fun KtNamedAnnotationValue.resolveStringConstantValue(): String? {
    return expression.let { it as? KtConstantAnnotationValue }?.constantValue
        ?.let { it as? KtStringConstantValue }
        ?.value
}

private fun KtNamedAnnotationValue.resolveBooleanConstantValue(): Boolean? {
    return expression.let { it as? KtConstantAnnotationValue }?.constantValue
        ?.let { it as? KtConstantValue.KtBooleanConstantValue }
        ?.value
}
