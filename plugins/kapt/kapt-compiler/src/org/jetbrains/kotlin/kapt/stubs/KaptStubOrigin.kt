/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.kapt.base.stubs.KotlinPosition
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName

/** Source declaration an ASM node of a stub was generated from, as the stub converter sees it; see [KaptIrOrigins]. */
abstract class KaptStubOrigin {
    abstract val firFile: FirFile?

    /** Position used to sort members and to map diagnostics back to Kotlin sources. */
    abstract val position: KotlinPosition?

    /** Source of the declaration, the KDoc comment is taken from it. */
    abstract val firSource: KtSourceElement?

    abstract val isConstructor: Boolean

    abstract val sourceAnnotations: List<KaptSourceAnnotation>
}

abstract class KaptClassOrigin : KaptStubOrigin() {
    abstract val packageFqName: FqName
    abstract val simpleName: String
    abstract val isNested: Boolean
    abstract val isInner: Boolean
    abstract val isEnumClass: Boolean
    abstract val isNestedInInterface: Boolean
    abstract val firClass: FirClass?

    override val isConstructor: Boolean get() = false
}

abstract class KaptFieldOrigin : KaptStubOrigin() {
    abstract val typeSource: KaptTypeSource
    abstract val firInitializer: FirExpression?
    abstract val firProperty: FirProperty?

    /** Java prefers a static field over a type name in `Foo.Companion.EnumClass`, so such initializers are dropped. */
    abstract val isTypeEnumInCompanion: Boolean

    override val isConstructor: Boolean get() = false
}

abstract class KaptMethodOrigin : KaptStubOrigin() {
    abstract val isJvmOverride: Boolean

    /** Names of the parameters except the dispatch receiver, `null` for special names. */
    abstract val parameterNames: List<String?>

    /** Default values of the parameters of the super class constructor called from a stub constructor, `null` if there is none. */
    abstract val superConstructorArgumentValues: List<Any?>?

    abstract val returnTypeSource: KaptTypeSource

    /** Source type of the JVM parameter at [index], or `null` if it can only be taken from the JVM signature. */
    abstract fun parameterTypeSource(index: Int, parameterCount: Int): KaptTypeSource?
}

class KaptTypeSource(
    val containsErrorTypes: Boolean,
    val typeRef: FirTypeRef?,
    val typeMappingMode: TypeMappingMode?,
    /** The `$completion` parameter of a suspend function, [typeRef] is the return type of the function. */
    val isContinuation: Boolean = false,
)

abstract class KaptSourceAnnotation {
    abstract val firAnnotation: FirAnnotation?
    abstract val typeContainsErrors: Boolean
    abstract val constantValue: ConstantValue<*>
}
