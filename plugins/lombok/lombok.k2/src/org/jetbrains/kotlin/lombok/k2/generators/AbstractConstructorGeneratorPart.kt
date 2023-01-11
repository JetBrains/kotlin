/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.callableIdForConstructor
import org.jetbrains.kotlin.utils.addToStdlib.runIf

abstract class AbstractConstructorGeneratorPart<T : ConeLombokAnnotations.ConstructorAnnotation>(private val session: FirSession) {
    protected val lombokService: LombokService
        get() = session.lombokService

    protected abstract fun getConstructorInfo(classSymbol: FirClassSymbol<*>): T?
    protected abstract fun getFieldsForParameters(classSymbol: FirClassSymbol<*>): List<FirJavaField>

    @OptIn(SymbolInternals::class)
    fun createConstructor(classSymbol: FirClassSymbol<*>): FirFunction? {
        val constructorInfo = getConstructorInfo(classSymbol) ?: return null
        val staticName = constructorInfo.staticName?.let { Name.identifier(it) }

        val substitutor: JavaTypeSubstitutor
        val constructorSymbol: FirFunctionSymbol<*>
        val builder = if (staticName == null) {
            FirJavaConstructorBuilder().apply {
                symbol = FirConstructorSymbol(classSymbol.classId.callableIdForConstructor()).also { constructorSymbol = it }
                classSymbol.fir.typeParameters.mapTo(typeParameters) {
                    buildConstructedClassTypeParameterRef { this.symbol = it.symbol }
                }
                substitutor = JavaTypeSubstitutor.Empty
                returnTypeRef = buildResolvedTypeRef {
                    type = classSymbol.defaultType()
                }
                isInner = classSymbol.isInner
                isPrimary = false
                isFromSource = true
                annotationBuilder = { emptyList() }
            }
        } else {
            FirJavaMethodBuilder().apply {
                name = staticName
                val methodSymbol = FirNamedFunctionSymbol(CallableId(classSymbol.classId, staticName)).also { constructorSymbol = it }
                symbol = methodSymbol

                val classTypeParameterSymbols = classSymbol.fir.typeParameters.map { it.symbol }
                classTypeParameterSymbols.mapTo(typeParameters) {
                    buildTypeParameterCopy(it.fir) {
                        this.symbol = FirTypeParameterSymbol()
                        containingDeclarationSymbol = methodSymbol
                    }
                }

                val javaClass = classSymbol.fir as FirJavaClass
                val javaTypeParametersFromClass = javaClass.javaTypeParameterStack
                    .filter { it.value in classTypeParameterSymbols }
                    .map { it.key }

                val functionTypeParameterToJavaTypeParameter = typeParameters.zip(javaTypeParametersFromClass)
                    .associate { (parameter, javaParameter) -> parameter.symbol to JavaTypeParameterStub(javaParameter) }

                for ((parameter, javaParameter) in functionTypeParameterToJavaTypeParameter) {
                    javaClass.javaTypeParameterStack.addParameter(javaParameter, parameter)
                }

                val javaTypeSubstitution: Map<JavaClassifier, JavaType> = javaTypeParametersFromClass
                    .zip(functionTypeParameterToJavaTypeParameter.values)
                    .associate { (originalParameter, newParameter) ->
                        originalParameter to JavaTypeParameterTypeStub(newParameter)
                    }

                substitutor = JavaTypeSubstitutorByMap(javaTypeSubstitution)
                returnTypeRef = buildResolvedTypeRef {
                    type = classSymbol.classId.defaultType(functionTypeParameterToJavaTypeParameter.keys.toList())
                }

                isStatic = true
                isFromSource = true
                annotationBuilder = { emptyList() }
            }
        }

        builder.apply {
            moduleData = classSymbol.moduleData
            status = FirResolvedDeclarationStatusImpl(
                constructorInfo.visibility,
                Modality.FINAL,
                constructorInfo.visibility.toEffectiveVisibility(classSymbol)
            ).apply {
                if (staticName != null) {
                    isStatic = true
                }
            }

            val fields = getFieldsForParameters(classSymbol)
            fields.mapTo(valueParameters) { field ->
                buildJavaValueParameter {
                    moduleData = field.moduleData
                    returnTypeRef = when (val typeRef = field.returnTypeRef) {
                        is FirJavaTypeRef -> buildJavaTypeRef {
                            type = substitutor.substituteOrSelf(typeRef.type)
                            annotationBuilder = { emptyList() }
                        }
                        else -> typeRef
                    }
                    containingFunctionSymbol = constructorSymbol
                    name = field.name
                    annotationBuilder = { emptyList() }
                    isVararg = false
                    isFromSource = true
                }
            }
        }

        return builder.build().apply {
            containingClassForStaticMemberAttr = classSymbol.toLookupTag()
        }
    }
}

private class JavaTypeParameterStub(val original: JavaTypeParameter) : JavaTypeParameter {
    override val name: Name
        get() = original.name
    override val isFromSource: Boolean
        get() = true
    override val annotations: Collection<JavaAnnotation>
        get() = original.annotations
    override val isDeprecatedInJavaDoc: Boolean
        get() = original.isDeprecatedInJavaDoc

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        return original.findAnnotation(fqName)
    }

    override val upperBounds: Collection<JavaClassifierType>
        get() = original.upperBounds
}

private class JavaClassifierTypeStub(
    val original: JavaClassifierType,
    override val typeArguments: List<JavaType?>,
) : JavaClassifierType {
    override val annotations: Collection<JavaAnnotation>
        get() = original.annotations
    override val isDeprecatedInJavaDoc: Boolean
        get() = original.isDeprecatedInJavaDoc
    override val classifier: JavaClassifier?
        get() = original.classifier
    override val isRaw: Boolean
        get() = original.isRaw
    override val classifierQualifiedName: String
        get() = original.classifierQualifiedName
    override val presentableText: String
        get() = original.presentableText
}

private class JavaTypeParameterTypeStub(
    override val classifier: JavaTypeParameter
) : JavaClassifierType {
    override val annotations: Collection<JavaAnnotation>
        get() = emptyList()
    override val isDeprecatedInJavaDoc: Boolean
        get() = false
    override val typeArguments: List<JavaType?>
        get() = emptyList()
    override val isRaw: Boolean
        get() = false
    override val classifierQualifiedName: String
        get() = classifier.name.identifier
    override val presentableText: String
        get() = classifierQualifiedName
}

private sealed class JavaTypeSubstitutor {
    object Empty : JavaTypeSubstitutor() {
        override fun substituteOrNull(type: JavaType): JavaType? {
            return null
        }
    }

    fun substituteOrSelf(type: JavaType): JavaType {
        return substituteOrNull(type) ?: type
    }

    abstract fun substituteOrNull(type: JavaType): JavaType?
}

private class JavaTypeSubstitutorByMap(val map: Map<JavaClassifier, JavaType>) : JavaTypeSubstitutor() {
    override fun substituteOrNull(type: JavaType): JavaType? {
        if (type !is JavaClassifierType) return null
        map[type.classifier]?.let { return it }
        var hasNewArguments = false
        val newArguments = type.typeArguments.map { argument ->
            if (argument == null) return@map null
            val newArgument = substituteOrSelf(argument)
            if (newArgument !== argument) {
                hasNewArguments = true
                newArgument
            } else {
                argument
            }
        }
        return runIf(hasNewArguments) {
            JavaClassifierTypeStub(type, newArguments)
        }
    }
}
