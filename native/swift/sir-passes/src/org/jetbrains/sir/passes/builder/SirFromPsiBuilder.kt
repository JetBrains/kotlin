/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.passes.builder

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.*
import org.jetbrains.kotlin.sir.util.SirSwiftModule


public fun KtAnalysisSession.buildSirDeclarationList(from: KtElement): List<SirDeclaration> {
    val res = mutableListOf<SirDeclaration>()
    from.accept(
        PsiToSirTranslationCollector(
            res,
            PsiToSirTranslatableChecker(this),
            PsiToSirElementTranslation(this),
        )
    )
    return res.toList()
}

private abstract class PsiToSirTranslation<T>(
    val analysisSession: KtAnalysisSession,
) : KtVisitor<T, Unit?>() {
    abstract override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit?): T
    abstract override fun visitNamedFunction(function: KtNamedFunction, data: Unit?): T
    abstract override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Unit?): T
    abstract override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit?): T
    abstract override fun visitProperty(property: KtProperty, data: Unit?): T
}

private class PsiToSirTranslationCollector(
    private val res: MutableList<SirDeclaration>,
    private val checker: PsiToSirTranslation<Boolean>,
    private val translator: PsiToSirTranslation<SirDeclaration>,
) : KtTreeVisitorVoid() {

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        classOrObject.checkAndTranslate(null)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        function.checkAndTranslate(null)
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        constructor.checkAndTranslate(null)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        constructor.checkAndTranslate(null)
    }

    override fun visitProperty(property: KtProperty) {
        property.checkAndTranslate(null)
    }

    private fun KtElement.checkAndTranslate(data: Unit?) = takeIf { it.accept(checker, data) }
        ?.let { res.add(it.accept(translator, data)) }
}

private class PsiToSirTranslatableChecker(
    analysisSession: KtAnalysisSession,
) : PsiToSirTranslation<Boolean>(analysisSession) {

    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit?): Boolean = with(analysisSession) {
        return classOrObject.isPublic
                && classOrObject.getNamedClassOrObjectSymbol()?.isConsumableBySirBuilder() ?: false
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Unit?): Boolean = with(analysisSession) {
        val functionIsPublicAndTopLevel = function.isPublic
                && !function.isAnonymous
        val functionSymbolIsTranslatable = (function.getFunctionLikeSymbol() as? KtFunctionSymbol)
            ?.let { symbol ->
                !symbol.isSuspend
                        && !symbol.isInline
                        && !symbol.isExtension
                        && !symbol.isOperator
            }
            ?: true
        return functionIsPublicAndTopLevel && functionSymbolIsTranslatable
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Unit?): Boolean {
        return constructor.isConsumable()
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit?): Boolean {
        return constructor.isConsumable()
    }

    override fun visitProperty(property: KtProperty, data: Unit?): Boolean {
        return property.isPublic
    }

    private fun KtConstructor<*>.isConsumable(): Boolean {
        return isPublic
    }
}

private class PsiToSirElementTranslation(
    analysisSession: KtAnalysisSession,
) : PsiToSirTranslation<SirDeclaration>(analysisSession) {

    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Unit?): SirDeclaration = with(analysisSession) {
        buildSirClassFromPsi(classOrObject)
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Unit?): SirDeclaration = with(analysisSession) {
        buildSirFunctionFromPsi(function)
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Unit?): SirDeclaration = with(analysisSession) {
        buildSirConstructorFromPsi(constructor)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: Unit?): SirDeclaration = with(analysisSession) {
        buildSirConstructorFromPsi(constructor)
    }

    override fun visitProperty(property: KtProperty, data: Unit?): SirDeclaration = with(analysisSession) {
        buildSirVariableFromPsi(property)
    }
}

context(KtAnalysisSession)
internal fun buildSirClassFromPsi(classOrObject: KtClassOrObject): SirNamedDeclaration {
    val symbol = classOrObject.getNamedClassOrObjectSymbol()!!
    return buildClass {
        name = classOrObject.name ?: "UNKNOWN_CLASS" // todo: error handling strategy: KT-65980
        origin = KotlinSource(symbol)

        classOrObject.acceptChildren(
            PsiToSirTranslationCollector(
                declarations,
                PsiToSirTranslatableChecker(analysisSession),
                PsiToSirElementTranslation(analysisSession),
            )
        )

        // HACK to support default constructors.
        // todo: We should rework builder from PSI to AnalysisApi during KT-66310
        val constructors = symbol.getMemberScope().getConstructors()
        if (constructors.count() == 1 && constructors.first().psi == classOrObject) {
            declarations.add(
                0,
                buildInit {
                    val constructorSymbol = constructors.first()
                    origin = KotlinSource(constructorSymbol)

                    kind = constructorSymbol.sirCallableKind
                    isFailable = false
                    initKind = SirInitializerKind.ORDINARY

                    constructorSymbol.valueParameters.mapTo(parameters) {
                        SirParameter(
                            argumentName = it.name.asString(),
                            type = buildSirNominalType(it.returnType)
                        )
                    }

                    documentation = null
                }
            )
        }
    }.also { resultedClass ->
        resultedClass.declarations.forEach { decl -> decl.parent = resultedClass }
    }
}

context(KtAnalysisSession)
internal fun buildSirFunctionFromPsi(function: KtNamedFunction): SirFunction = buildFunction {
    val symbol = function.getFunctionLikeSymbol()
    val callableId = symbol.callableIdIfNonLocal
    origin = KotlinSource(symbol)

    kind = symbol.sirCallableKind

    name = callableId?.callableName?.asString() ?: "UNKNOWN_FUNCTION_NAME"

    symbol.valueParameters.mapTo(parameters) {
        SirParameter(
            argumentName = it.name.asString(),
            type = buildSirNominalType(it.returnType)
        )
    }
    returnType = buildSirNominalType(symbol.returnType)
    documentation = function.docComment?.text
}

context(KtAnalysisSession)
internal fun buildSirConstructorFromPsi(function: KtConstructor<*>): SirInit = buildInit {
    val symbol = function.getConstructorSymbol()
    origin = KotlinSource(symbol)

    kind = symbol.sirCallableKind
    isFailable = false
    initKind = SirInitializerKind.ORDINARY

    symbol.valueParameters.mapTo(parameters) {
        SirParameter(
            argumentName = it.name.asString(),
            type = buildSirNominalType(it.returnType)
        )
    }

    documentation = function.docComment?.text
}

context(KtAnalysisSession)
internal fun buildSirVariableFromPsi(variable: KtProperty): SirVariable = buildVariable {
    val symbol = variable.getVariableSymbol()
    val callableId = symbol.callableIdIfNonLocal
    origin = KotlinSource(symbol)

    val accessorKind = symbol.sirCallableKind

    name = callableId?.callableName?.asString() ?: "UNKNOWN_VARIABLE_NAME"

    type = buildSirNominalType(symbol.returnType)

    getter = buildGetter {
        kind = accessorKind
    }
    setter = if (variable.isVar) buildSetter {
        kind = accessorKind
    } else null
}.also {
    it.getter.parent = it
    it.setter?.parent = it
}

public data class KotlinSource(
    val symbol: KtSymbol,
) : SirOrigin.Foreign.SourceCode


context(KtAnalysisSession)
private fun buildSirNominalType(it: KtType): SirNominalType = SirNominalType(
    when {
        it.isUnit -> SirSwiftModule.void

        it.isByte -> SirSwiftModule.int8
        it.isShort -> SirSwiftModule.int16
        it.isInt -> SirSwiftModule.int32
        it.isLong -> SirSwiftModule.int64

        it.isUByte -> SirSwiftModule.uint8
        it.isUShort -> SirSwiftModule.uint16
        it.isUInt -> SirSwiftModule.uint32
        it.isULong -> SirSwiftModule.uint64

        it.isBoolean -> SirSwiftModule.bool

        it.isDouble -> SirSwiftModule.double
        it.isFloat -> SirSwiftModule.float
        else ->
            throw IllegalArgumentException("Swift Export does not support argument type: ${it.asStringForDebugging()}")
    }
)

context(KtAnalysisSession)
private fun KtNamedClassOrObjectSymbol.isConsumableBySirBuilder(): Boolean =
    classKind == KtClassKind.CLASS
            && (superTypes.count() == 1 && superTypes.first().isAny) // Every class has Any as a superclass
            && !isData
            && !isInline
            && modality == Modality.FINAL

private val KtCallableSymbol.sirCallableKind: SirCallableKind
    get() = when (symbolKind) {
        KtSymbolKind.TOP_LEVEL -> {
            val isRootPackage = callableIdIfNonLocal?.packageName?.isRoot
            if (isRootPackage == true) {
                SirCallableKind.FUNCTION
            } else {
                SirCallableKind.STATIC_METHOD
            }
        }
        KtSymbolKind.CLASS_MEMBER, KtSymbolKind.ACCESSOR
        -> SirCallableKind.INSTANCE_METHOD
        KtSymbolKind.LOCAL,
        KtSymbolKind.SAM_CONSTRUCTOR
        -> TODO("encountered callable kind($symbolKind) that is not translatable currently. Fix this crash during KT-65980.")
    }
