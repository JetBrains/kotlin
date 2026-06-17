/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirNamedFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.plugin.tryGeneratingNoArgDelegatingConstructorCall
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.fir.types.withReplacedConeType
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.k2.generators.kotlin.isRelevantForConflictsCheck
import org.jetbrains.kotlin.lombok.k2.generators.kotlin.tryBuildingJvmStaticAnnotationCall
import org.jetbrains.kotlin.lombok.k2.java.JavaTypeParameterStub
import org.jetbrains.kotlin.lombok.k2.java.JavaTypeParameterTypeStub
import org.jetbrains.kotlin.lombok.k2.java.JavaTypeSubstitutor
import org.jetbrains.kotlin.lombok.k2.java.JavaTypeSubstitutorByMap
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.callableIdForConstructor
import org.jetbrains.kotlin.types.Variance

abstract class AbstractConstructorGeneratorPart<T : ConeLombokAnnotations.ConstructorAnnotation>(private val session: FirSession) {
    protected val lombokService: LombokService
        get() = session.lombokService

    abstract fun getConstructorInfo(classSymbol: FirClassSymbol<*>): T?
    protected abstract fun getFieldsForParameters(classSymbol: FirClassSymbol<*>): List<FirJavaField>

    @OptIn(DirectDeclarationsAccess::class)
    protected fun containsExplicitConstructor(classSymbol: FirClassSymbol<*>): Boolean {
        return classSymbol.declarationSymbols.any { it is FirConstructorSymbol && it.source?.kind is KtRealSourceElementKind }
    }

    /**
     * Checks clashing with generated or explicit constructors according to Lombok logic;
     * Vararg value parameter from an explicit constructor never causes a conflict.
     * Value parameters from generated functions can never be vararg.
     */
    private fun FirFunctionSymbol<*>.checkParametersClashing(valueParametersCount: Int): Boolean {
        return valueParameterSymbols.none { it.isVararg } && valueParameterSymbols.size == valueParametersCount
    }

    /**
     * Checks clashing with already generated constructors (regular or static functions).
     * The generated constructors don't have vararg parameters, so don't check them.
     */
    private inline fun <reified T : FirFunction> MutableList<FirFunction>.checkClashing(valueParametersCount: Int): Boolean {
        return any { it is T && it.symbol.checkParametersClashing(valueParametersCount) }
    }

    @OptIn(SymbolInternals::class)
    fun MutableList<FirFunction>.addIfNonClashing(classSymbol: FirClassSymbol<*>, declaredScope: FirClassDeclaredMemberScope?) {
        val constructorInfo: T
        val targetClassSymbol: FirClassSymbol<*>

        if (classSymbol.isCompanion) {
            // Create static constructors (when `staticName` is specified) inside companions.
            val outerClass = classSymbol.classId.outerClassId?.toSymbol(session) as? FirRegularClassSymbol ?: return
            constructorInfo = getConstructorInfo(outerClass) ?: return
            targetClassSymbol = outerClass
        } else {
            constructorInfo = getConstructorInfo(classSymbol) ?: return
            targetClassSymbol = classSymbol
        }

        val visibility = constructorInfo.visibility ?: return
        val fields = getFieldsForParameters(targetClassSymbol)
        val valuesParameterCount = fields.size
        val staticName = constructorInfo.staticName?.let { Name.identifier(it) }
        val hasJavaOrigin = classSymbol.hasJavaOrigin

        require(hasJavaOrigin || fields.isEmpty()) {
            "Kotlin supports only `@NoArgsConstructor` annotations, that's why `fields` expected to be empty."
        }

        // Checks clashing with explicit constructors, vararg doesn't cause a conflict
        fun FirFunctionSymbol<*>.checkParametersClashing(): Boolean {
            return valueParameterSymbols.let { parameters ->
                parameters.none { it.isVararg } && parameters.size == valuesParameterCount
            }
        }

        val substitutor: JavaTypeSubstitutor
        val constructorSymbol: FirFunctionSymbol<*>
        var returnTarget: FirFunctionTarget? = null

        val builder = if (staticName == null || (!hasJavaOrigin && !classSymbol.isCompanion)) {
            // Generate a regular constructor in Kotlin classes even if `staticName` is specified
            // Because in the latter case, we create a companion object with a function that calls the regular constructor
            if (checkClashing<FirConstructor>(valuesParameterCount)) return

            var hasConflict = false
            declaredScope?.processDeclaredConstructors { constructor ->
                hasConflict = hasConflict || constructor.checkParametersClashing(valuesParameterCount)
            }
            if (hasConflict) return

            val builder = if (classSymbol.hasJavaOrigin) {
                FirJavaConstructorBuilder().apply {
                    containingClassSymbol = targetClassSymbol
                    isPrimary = false
                    isFromSource = true
                }
            } else {
                FirConstructorBuilder().apply {
                    isLocal = false
                    origin = FirDeclarationOrigin.Plugin(ConstructorGeneratorKey)
                    delegatedConstructor = targetClassSymbol.tryGeneratingNoArgDelegatingConstructorCall(session) ?: return
                }
            }

            builder.apply {
                symbol = FirConstructorSymbol(targetClassSymbol.classId.callableIdForConstructor()).also { constructorSymbol = it }
                targetClassSymbol.fir.typeParameters.mapTo(typeParameters) {
                    buildConstructedClassTypeParameterRef { this.symbol = it.symbol }
                }
                substitutor = JavaTypeSubstitutor.Empty
                returnTypeRef = buildResolvedTypeRef {
                    coneType = targetClassSymbol.defaultType()
                }
            }
        } else {
            if (checkClashing<FirNamedFunction>(valuesParameterCount)) return

            var hasConflict = false
            declaredScope?.processFunctionsByName(staticName) { function ->
                hasConflict = hasConflict || (function.isRelevantForConflictsCheck && function.checkParametersClashing(valuesParameterCount))
            }
            if (hasConflict) return

            val methodSymbol = FirNamedFunctionSymbol(CallableId(targetClassSymbol.classId, staticName)).also { constructorSymbol = it }

            if (hasJavaOrigin) {
                FirJavaMethodBuilder().apply {
                    containingClassSymbol = targetClassSymbol
                    name = staticName
                    symbol = methodSymbol
                    isFromSource = true

                    val classTypeParameterSymbols = targetClassSymbol.fir.typeParameters.map { it.symbol }
                    classTypeParameterSymbols.copyTypeParametersTo(typeParameters, methodSymbol)

                    val javaClass = targetClassSymbol.fir as FirJavaClass
                    val javaTypeParametersFromClass = javaClass.classJavaTypeParameterStack
                        .filter { it.value in classTypeParameterSymbols }
                        .map { it.key }

                    val functionTypeParameterToJavaTypeParameter = typeParameters.zip(javaTypeParametersFromClass)
                        .associate { [parameter, javaParameter] -> parameter.symbol to JavaTypeParameterStub(javaParameter) }

                    for ([parameter, javaParameter] in functionTypeParameterToJavaTypeParameter) {
                        javaClass.classJavaTypeParameterStack.addParameter(javaParameter, parameter)
                    }

                    val javaTypeSubstitution: Map<JavaClassifier, JavaType> = javaTypeParametersFromClass
                        .zip(functionTypeParameterToJavaTypeParameter.values)
                        .associate { [originalParameter, newParameter] ->
                            originalParameter to JavaTypeParameterTypeStub(newParameter)
                        }

                    substitutor = JavaTypeSubstitutorByMap(javaTypeSubstitution)
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = targetClassSymbol.classId.defaultType(functionTypeParameterToJavaTypeParameter.keys.toList())
                    }
                }
            } else {
                FirNamedFunctionBuilder().apply {
                    name = staticName
                    symbol = methodSymbol
                    isLocal = false
                    origin = FirDeclarationOrigin.Plugin(ConstructorGeneratorKey)
                    substitutor = JavaTypeSubstitutor.Empty

                    val classTypeParameterSymbols = targetClassSymbol.fir.typeParameters.map { it.symbol }
                    val substitution = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
                    classTypeParameterSymbols.copyTypeParametersTo(typeParameters, methodSymbol, substitution)
                    remapTypeParameterBounds(typeParameters, substitutorByMap(substitution, session))

                    val functionTypeParameterSymbols = typeParameters.map { it.symbol }
                    val constructedType = targetClassSymbol.classId.defaultType(functionTypeParameterSymbols)

                    returnTypeRef = buildResolvedTypeRef {
                        coneType = constructedType
                    }
                    dispatchReceiverType = classSymbol.defaultType()

                    annotations.add(methodSymbol.tryBuildingJvmStaticAnnotationCall(session) ?: return)

                    // The regular constructor should be either declared explicitly or generated by this generator (previous branch)
                    val regularEmptyConstructor =
                        targetClassSymbol.constructors(session).singleOrNull { it.valueParameterSymbols.isEmpty() }
                            ?: return

                    body = buildSingleExpressionBlock(
                        buildReturnExpression {
                            returnTarget = FirFunctionTarget(labelName = null, isLambda = false)
                            target = returnTarget
                            result = buildFunctionCall {
                                calleeReference = buildResolvedNamedReference {
                                    name = targetClassSymbol.name
                                    resolvedSymbol = regularEmptyConstructor
                                }
                                functionTypeParameterSymbols.mapTo(typeArguments) { typeParameterSymbol ->
                                    buildTypeProjectionWithVariance {
                                        typeRef = buildResolvedTypeRef {
                                            coneType = typeParameterSymbol.toConeType()
                                        }
                                        variance = Variance.INVARIANT
                                    }
                                }
                                coneTypeOrNull = constructedType
                            }
                        }
                    )
                }
            }
        }

        builder.apply {
            // The plugin-generated source is needed to prevent reporting of `PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED`
            // in the same way as the no-arg plugin works. Also, see KT-80651
            source = targetClassSymbol.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated.Default)
            moduleData = targetClassSymbol.moduleData
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(targetClassSymbol)
            ).apply {
                if (staticName != null && hasJavaOrigin) {
                    // Don't specify the property for Kotlin static constructors because Kotlin doesn't support companion blocks yet.
                    // As a workaround, generate a function inside a companion object and mark it with `@JvmStatic`
                    // (in the way as Logger generator works).
                    isStatic = true
                }
            }

            fields.mapTo(valueParameters) { field ->
                buildJavaValueParameter {
                    moduleData = field.moduleData
                    returnTypeRef = when (val typeRef = field.returnTypeRef) {
                        is FirJavaTypeRef -> buildJavaTypeRef {
                            type = substitutor.substituteOrSelf(typeRef.type)
                            annotationBuilder = { emptyList() }
                            source = targetClassSymbol.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
                        }
                        else -> typeRef
                    }
                    containingDeclarationSymbol = constructorSymbol
                    name = field.name
                    isVararg = false
                    isFromSource = true
                }
            }
        }

        add(builder.build().apply {
            containingClassForStaticMemberAttr = targetClassSymbol.toLookupTag()
            returnTarget?.bind(this)
        })
    }

    @OptIn(SymbolInternals::class)
    private fun List<FirTypeParameterSymbol>.copyTypeParametersTo(
        destination: MutableList<FirTypeParameter>,
        methodSymbol: FirFunctionSymbol<*>,
        substitution: MutableMap<FirTypeParameterSymbol, ConeKotlinType>? = null,
    ) = mapTo(destination) { classTypeParameter ->
        buildTypeParameterCopy(classTypeParameter.fir) {
            this.symbol = FirTypeParameterSymbol()
            containingDeclarationSymbol = methodSymbol
        }.also { copy -> substitution?.put(classTypeParameter, copy.symbol.toConeType()) }
    }

    private fun remapTypeParameterBounds(typeParameters: List<FirTypeParameter>, substitutor: ConeSubstitutor) =
        typeParameters.forEach { typeParameter ->
            val remappedBounds = typeParameter.bounds.map { bound ->
                bound.withReplacedConeType(substitutor.substituteOrNull(bound.coneType))
            }
            typeParameter.replaceBounds(remappedBounds)
        }
}
