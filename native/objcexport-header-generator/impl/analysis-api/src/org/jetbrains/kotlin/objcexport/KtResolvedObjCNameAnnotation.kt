/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
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
internal fun KtAnnotatedSymbol.resolveObjCNameAnnotation(): KtResolvedObjCNameAnnotation {
    var objCName: String? = null
    var swiftName: String? = null
    var isExact = false

    annotationsList.annotations.find { it.classId?.asSingleFqName() == KonanFqNames.objCName }?.let { annotation ->
        annotation.arguments.forEach { argument ->
            when (argument.name.identifier) {
                "name" -> objCName = argument.expression.let { it as? KtConstantAnnotationValue }
                    ?.constantValue?.let { it as KtStringConstantValue }
                    ?.value
                "swiftName" -> swiftName = argument.expression.let { it as? KtConstantAnnotationValue }
                    ?.constantValue?.let { it as KtStringConstantValue }
                    ?.value
                "exact" -> isExact = argument.expression.let { it as? KtConstantAnnotationValue }
                    ?.constantValue?.let { it as KtConstantValue.KtBooleanConstantValue }
                    ?.value ?: isExact
            }
        }
    }

    return KtResolvedObjCNameAnnotation(
        objCName = objCName,
        swiftName = swiftName,
        isExact = isExact
    )
}
