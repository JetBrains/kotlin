/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue.BooleanValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue.StringValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
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

internal fun KaAnnotatedSymbol.resolveObjCNameAnnotation(): KtResolvedObjCNameAnnotation? {
    val annotation = annotations.find { it.classId?.asSingleFqName() == KonanFqNames.objCName } ?: return null

    return KtResolvedObjCNameAnnotation(
        objCName = annotation.findArgument("name")?.resolveStringConstantValue(),
        swiftName = annotation.findArgument("swiftName")?.resolveStringConstantValue(),
        isExact = annotation.findArgument("exact")?.resolveBooleanConstantValue() ?: false
    )
}

private fun KaAnnotation.findArgument(name: String): KaNamedAnnotationValue? {
    return arguments.find { it.name.identifier == name }
}

private fun KaNamedAnnotationValue.resolveStringConstantValue(): String? {
    return expression.let { it as? KaAnnotationValue.ConstantValue }?.value
        ?.let { it as? StringValue }
        ?.value
}

private fun KaNamedAnnotationValue.resolveBooleanConstantValue(): Boolean? {
    return expression.let { it as? KaAnnotationValue.ConstantValue }?.value
        ?.let { it as? BooleanValue }
        ?.value
}
