/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirConstructorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameterCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.plugin.generateNoArgDelegatingConstructorCall
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
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
        val constructorInfo = getConstructorInfo(classSymbol) ?: return
        val visibility = constructorInfo.visibility ?: return
        val fields = getFieldsForParameters(classSymbol)
        val valuesParameterCount = fields.size
        val staticName = constructorInfo.staticName?.let { Name.identifier(it) }

        val substitutor: JavaTypeSubstitutor
        val constructorSymbol: FirFunctionSymbol<*>
        val builder = if (staticName == null) {
            if (checkClashing<FirConstructor>(valuesParameterCount)) return

            var hasConflict = false
            declaredScope?.processDeclaredConstructors { constructor ->
                hasConflict = hasConflict || constructor.checkParametersClashing(valuesParameterCount)
            }
            if (hasConflict) return

            val builder = if (classSymbol.hasJavaOrigin) {
                FirJavaConstructorBuilder().apply {
                    containingClassSymbol = classSymbol
                    isPrimary = false
                    isFromSource = true
                }
            } else {
                FirConstructorBuilder().apply {
                    isLocal = false
                    origin = FirDeclarationOrigin.Plugin(ConstructorGeneratorKey)
                    delegatedConstructor = classSymbol.generateNoArgDelegatingConstructorCall(session)
                }
            }

            builder.apply {
                symbol = FirConstructorSymbol(classSymbol.classId.callableIdForConstructor()).also { constructorSymbol = it }
                classSymbol.fir.typeParameters.mapTo(typeParameters) {
                    buildConstructedClassTypeParameterRef { this.symbol = it.symbol }
                }
                substitutor = JavaTypeSubstitutor.Empty
                returnTypeRef = buildResolvedTypeRef {
                    coneType = classSymbol.defaultType()
                }
            }
        } else {
            if (checkClashing<FirNamedFunction>(valuesParameterCount)) return

            var hasConflict = false
            declaredScope?.processFunctionsByName(staticName) { function ->
                hasConflict = hasConflict || function.checkParametersClashing(valuesParameterCount)
            }
            if (hasConflict) return

            FirJavaMethodBuilder().apply {
                containingClassSymbol = classSymbol
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
                    coneType = classSymbol.classId.defaultType(functionTypeParameterToJavaTypeParameter.keys.toList())
                }

                isFromSource = true
            }
        }

        builder.apply {
            // The plugin-generated source is needed to prevent reporting of `PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED`
            // in the same way as the no-arg plugin works. Also, see KT-80651
            source = classSymbol.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated.Default)
            moduleData = classSymbol.moduleData
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                Modality.FINAL,
                visibility.toEffectiveVisibility(classSymbol)
            ).apply {
                if (staticName != null) {
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
                            source = classSymbol.source?.fakeElement(KtFakeSourceElementKind.Enhancement)
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
            containingClassForStaticMemberAttr = classSymbol.toLookupTag()
        })
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
    override val classifier: JavaTypeParameter,
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
