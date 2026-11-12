/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.kapt.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt.stubs.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName

/** Matched to annotation nodes by [constantValue], like the IR annotations, so that the converter makes the same choices. */
internal class FirKaptSourceAnnotation(
    override val firAnnotation: FirAnnotation,
    override val typeContainsErrors: Boolean,
    computeConstantValue: () -> ConstantValue<*>,
) : KaptSourceAnnotation() {
    override val constantValue: ConstantValue<*> by lazy(LazyThreadSafetyMode.NONE, computeConstantValue)
}

internal class FirKaptClassOrigin(
    override val firFile: FirFile?,
    override val position: KotlinPosition?,
    override val firSource: KtSourceElement?,
    override val sourceAnnotations: List<KaptSourceAnnotation>,
    override val packageFqName: FqName,
    override val simpleName: String,
    override val isNested: Boolean,
    override val isInner: Boolean,
    override val isEnumClass: Boolean,
    override val isNestedInInterface: Boolean,
    override val firClass: FirClass?,
) : KaptClassOrigin() {
    override fun toString(): String = "FIR class ${packageFqName.child(org.jetbrains.kotlin.name.Name.guessByFirstCharacter(simpleName))}"
}

internal class FirKaptFieldOrigin(
    private val declaration: FirDeclaration,
    override val firFile: FirFile,
    override val position: KotlinPosition?,
    override val firSource: KtSourceElement?,
    override val sourceAnnotations: List<KaptSourceAnnotation>,
    override val typeSource: KaptTypeSource,
    override val firInitializer: FirExpression?,
    override val firProperty: FirProperty?,
    override val isTypeEnumInCompanion: Boolean,
) : KaptFieldOrigin() {
    override fun toString(): String = "FIR field of $declaration"
}

internal class FirKaptMethodOrigin(
    private val declaration: FirDeclaration?,
    override val firFile: FirFile,
    override val position: KotlinPosition?,
    override val firSource: KtSourceElement?,
    override val isConstructor: Boolean,
    override val sourceAnnotations: List<KaptSourceAnnotation>,
    override val isJvmOverride: Boolean,
    override val parameterNames: List<String?>,
    private val superConstructorArguments: () -> List<Any?>?,
    override val returnTypeSource: KaptTypeSource,
    /** Source types of the JVM parameters, `null` where only the JVM signature is known. */
    private val parameterTypeSources: List<KaptTypeSource?>,
) : KaptMethodOrigin() {
    override val superConstructorArgumentValues: List<Any?>?
        get() = superConstructorArguments()

    override fun parameterTypeSource(index: Int, parameterCount: Int): KaptTypeSource? =
        if (parameterCount == parameterTypeSources.size) parameterTypeSources.getOrNull(index) else null

    fun withPosition(position: KotlinPosition?): FirKaptMethodOrigin = FirKaptMethodOrigin(
        declaration, firFile, position, firSource, isConstructor, sourceAnnotations, isJvmOverride, parameterNames,
        superConstructorArguments, returnTypeSource, parameterTypeSources,
    )

    override fun toString(): String = "FIR method of $declaration"
}

internal fun typeSource(containsErrorTypes: Boolean, typeRef: FirTypeRef?, mode: TypeMappingMode?, isContinuation: Boolean = false) =
    KaptTypeSource(containsErrorTypes, typeRef, mode, isContinuation)
