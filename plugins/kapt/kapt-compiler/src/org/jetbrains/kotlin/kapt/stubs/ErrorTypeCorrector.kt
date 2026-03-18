/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt.stubs

import com.intellij.psi.util.PsiTreeUtil
import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmTypeMapper
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.kapt.base.javac.kaptError
import org.jetbrains.kotlin.kapt.base.mapJList
import org.jetbrains.kotlin.kapt.base.mapJListIndexed
import org.jetbrains.kotlin.kapt.stubs.ErrorTypeCorrector.TypeKind.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import com.sun.tools.javac.util.List as JavacList

private typealias SubstitutionMap = Map<String, Triple<KtTypeParameter, KtTypeProjection, ConeTypeProjection?>>

class ErrorTypeCorrector(
    private val converter: KaptStubConverter,
    private val typeKind: TypeKind,
    file: KtFile,
) {
    private val defaultType = converter.treeMaker.FqName(Any::class.java.name)

    private val treeMaker get() = converter.treeMaker

    private val aliasedImports = mutableMapOf<String, JCTree.JCExpression>().apply {
        for (importDirective in file.importDirectives) {
            if (importDirective.isAllUnder) continue

            val aliasName = importDirective.aliasName ?: continue
            val importedFqName = importDirective.importedFqName ?: continue

            this[aliasName] = treeMaker.FqName(importedFqName)
        }
    }

    enum class TypeKind {
        RETURN_TYPE, METHOD_PARAMETER_TYPE, SUPER_TYPE, ANNOTATION
    }

    fun convert(type: KtTypeElement): JCTree.JCExpression {
        val typeReference = PsiTreeUtil.getParentOfType(type, KtTypeReference::class.java, true)
        val coneType = typeReference?.let(converter.typeReferenceToFirType::get)

        return convert(type, coneType, emptyMap())
    }

    private fun convert(type: KtTypeElement, coneType: ConeKotlinType?, substitutions: SubstitutionMap): JCTree.JCExpression {
        return when (type) {
            is KtUserType -> convertUserType(type, coneType, substitutions)
            is KtNullableType -> convert(type.innerType ?: return defaultType, coneType, substitutions)
            is KtFunctionType -> convertFunctionType(type, coneType, substitutions)
            else -> defaultType
        }
    }

    private fun convert(typeReference: KtTypeReference?, coneType: ConeKotlinType?, substitutions: SubstitutionMap): JCTree.JCExpression {
        val type = typeReference?.typeElement ?: return defaultType
        return convert(type, coneType, substitutions)
    }

    private fun convertUserType(type: KtUserType, coneType: ConeKotlinType?, substitutions: SubstitutionMap): JCTree.JCExpression {
        if (coneType != null) {
            convertFirUserType(type, coneType, substitutions)?.let { return it }
        }

        val referencedName = type.referencedName ?: return defaultType
        val qualifier = type.qualifier

        if (qualifier == null) {
            if (referencedName in substitutions) {
                val (typeParameter, projection) = substitutions.getValue(referencedName)
                return convertTypeProjection(projection, null, typeParameter.variance, emptyMap())
            }

            aliasedImports[referencedName]?.let { return it }
        }

        val baseExpression = when {
            qualifier != null -> {
                val qualifierType = convertUserType(qualifier, null, substitutions)
                if (qualifierType === defaultType) return defaultType // Do not allow to use 'defaultType' as a qualifier
                treeMaker.Select(qualifierType, treeMaker.name(referencedName))
            }

            else -> treeMaker.SimpleName(referencedName)
        }

        val arguments = type.typeArguments
        if (arguments.isEmpty()) return baseExpression

        return treeMaker.TypeApply(
            baseExpression,
            SimpleClassicTypeSystemContext.convertTypeArguments(
                arguments, null, ErrorUtils.createErrorType(ErrorTypeKind.KAPT_ERROR_TYPE), substitutions
            ),
        )
    }

    private fun TypeSystemCommonBackendContext.convertTypeArguments(
        arguments: List<KtTypeProjection>,
        typeParameters: List<TypeParameterMarker>?,
        type: KotlinTypeMarker,
        substitutions: SubstitutionMap,
    ): JavacList<JCTree.JCExpression> = mapJListIndexed(arguments) { index, projection ->
        val typeMappingMode = when (typeKind) {
            //TODO figure out if the containing method is an annotation method
            RETURN_TYPE -> getOptimalModeForReturnType(type, false)
            METHOD_PARAMETER_TYPE -> getOptimalModeForValueParameter(type)
            SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
            ANNOTATION -> TypeMappingMode.DEFAULT // see genAnnotation in org/jetbrains/kotlin/codegen/AnnotationCodegen.java
        }.updateArgumentModeFromAnnotations(type, this)

        val typeParameter = typeParameters?.getOrNull(index)
        val typeArgument = type.getArguments().getOrNull(index)
        val variance = if (typeArgument != null && typeParameter != null && !typeArgument.isStarProjection()) {
            with(KotlinTypeMapper) {
                getVarianceForWildcard(typeParameter, typeArgument, typeMappingMode)
            }
        } else {
            null
        }
        convertTypeProjection(projection, typeArgument as? ConeTypeProjection, variance, substitutions)
    }

    private fun convertTypeProjection(
        projection: KtTypeProjection,
        coneProjection: ConeTypeProjection?,
        variance: Variance?,
        substitutions: SubstitutionMap
    ): JCTree.JCExpression {
        fun unbounded() = treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)

        // Use unbounded wildcard when a generic argument can't be resolved
        val argumentType = projection.typeReference ?: return unbounded()
        val coneArgumentType = (coneProjection as? ConeKotlinTypeProjection)?.type
        val argumentExpression by lazy { convert(argumentType, coneArgumentType, substitutions) }

        if (variance === Variance.INVARIANT) {
            return argumentExpression
        }

        val projectionKind = projection.projectionKind

        return when {
            projectionKind === KtProjectionKind.STAR -> treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.UNBOUND), null)
            projectionKind === KtProjectionKind.IN || variance === Variance.IN_VARIANCE ->
                treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.SUPER), argumentExpression)

            projectionKind === KtProjectionKind.OUT || variance === Variance.OUT_VARIANCE ->
                treeMaker.Wildcard(treeMaker.TypeBoundKind(BoundKind.EXTENDS), argumentExpression)

            else -> argumentExpression // invariant
        }
    }

    private fun convertFunctionType(type: KtFunctionType, coneType: ConeKotlinType?, substitutions: SubstitutionMap): JCTree.JCExpression {
        val receiverType = type.receiverTypeReference
        val coneTypeArguments = (coneType as? ConeClassLikeType)?.typeArguments
        var parameterTypes = mapJList(type.parameters.withIndex()) { (index, parameterKtType) ->
            convert(
                parameterKtType.typeReference,
                (coneTypeArguments?.getOrNull(index + if (receiverType != null) 1 else 0) as? ConeKotlinTypeProjection)?.type,
                substitutions
            )
        }
        val returnType = convert(
            type.returnTypeReference,
            (coneTypeArguments?.lastOrNull() as? ConeKotlinTypeProjection)?.type,
            substitutions,
        )

        if (receiverType != null) {
            parameterTypes = parameterTypes.prepend(
                convert(receiverType, (coneTypeArguments?.firstOrNull() as? ConeKotlinTypeProjection)?.type, substitutions)
            )
        }

        parameterTypes = parameterTypes.append(returnType)

        val treeMaker = converter.treeMaker
        return treeMaker.TypeApply(treeMaker.SimpleName("Function" + (parameterTypes.size - 1)), parameterTypes)
    }

    private fun KtTypeParameterListOwner.getSubstitutions(actualType: KtUserType, coneType: ConeKotlinType?): SubstitutionMap {
        val arguments = actualType.typeArguments

        if (typeParameters.size != arguments.size) {
            val kaptContext = converter.kaptContext
            val error = kaptContext.kaptError("${typeParameters.size} parameters are expected but ${arguments.size} passed")
            kaptContext.compiler.log.report(error)
            return emptyMap()
        }

        val substitutionMap = mutableMapOf<String, Triple<KtTypeParameter, KtTypeProjection, ConeTypeProjection?>>()

        typeParameters.forEachIndexed { index, typeParameter ->
            val name = typeParameter.name ?: return@forEachIndexed
            substitutionMap[name] = Triple(typeParameter, arguments[index], coneType?.typeArguments?.getOrNull(index))
        }

        return substitutionMap
    }

    @OptIn(SymbolInternals::class)
    private fun convertFirUserType(type: KtUserType, coneType: ConeKotlinType, substitutions: SubstitutionMap): JCTree.JCExpression? {
        val session = converter.kaptContext.firSession!!

        val abbreviatedType = coneType.abbreviatedType

        val baseExpression = when {
            abbreviatedType != null && coneType is ConeErrorType -> {
                val firTypeAlias = abbreviatedType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId) as? FirTypeAliasSymbol
                val typeAlias = firTypeAlias?.fir?.realPsi as? KtTypeAlias
                val actualType = typeAlias?.getTypeReference() ?: return defaultType
                val newSubstitutions = typeAlias.getSubstitutions(type, abbreviatedType)
                return convert(actualType, converter.typeReferenceToFirType[actualType], newSubstitutions)
            }
            coneType is ConeClassLikeType && coneType !is ConeErrorType -> {
                // We only get here if some type were an error type. In other words, 'type' is either an error type or its argument,
                // so it's impossible it to be unboxed primitive.
                val asmType = FirJvmTypeMapper(session).mapType(coneType, TypeMappingMode.GENERIC_ARGUMENT)
                converter.treeMaker.Type(asmType)
            }
            else -> {
                val referencedName = type.referencedName ?: return defaultType
                val qualifier = type.qualifier

                if (qualifier == null) {
                    if (referencedName in substitutions) {
                        val (typeParameter, projection, coneProjection) = substitutions.getValue(referencedName)
                        return convertTypeProjection(projection, coneProjection, typeParameter.variance, emptyMap())
                    }

                    aliasedImports[referencedName]?.let { return it }
                }

                when {
                    qualifier != null -> {
                        // Technically this is incorrect, but it doesn't affect anything because we're here only in case of an error type,
                        // which will be corrected recursively only by using PSI anyway. So any ConeKotlinType should be fine here.
                        val outerType = coneType

                        val qualifierType = convertUserType(qualifier, outerType, substitutions)
                        if (qualifierType === defaultType) return defaultType // Do not allow to use 'defaultType' as a qualifier
                        treeMaker.Select(qualifierType, treeMaker.name(referencedName))
                    }

                    else -> treeMaker.SimpleName(referencedName)
                }
            }
        }

        val arguments = type.typeArguments
        if (arguments.isEmpty()) return baseExpression

        val typeParameters =
            coneType.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)?.typeParameterSymbols?.map { it.toLookupTag() }
        return treeMaker.TypeApply(
            baseExpression,
            session.typeContext.convertTypeArguments(arguments, typeParameters, coneType, substitutions),
        )
    }
}

fun KotlinType.containsErrorTypes(allowedDepth: Int = 10): Boolean {
    // Need to limit recursion depth in case of complex recursive generics
    if (allowedDepth <= 0) {
        return false
    }

    if (this.isError) return true
    if (this.arguments.any { !it.isStarProjection && it.type.containsErrorTypes(allowedDepth - 1) }) return true
    return false
}
