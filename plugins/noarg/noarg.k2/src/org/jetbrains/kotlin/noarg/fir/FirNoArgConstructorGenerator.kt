/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.getDeprecationsProviderFromAnnotations
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.noarg.NoArgPluginKey
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf

private const val NO_ARG_CONSTRUCTOR_HIDDEN_DEPRECATED_MESSAGE: String =
    "No-arg constructor is hidden from direct usage"

internal class FirNoArgConstructorGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private fun FirRegularClassSymbol.isAnnotatedWithNoArg(): Boolean =
        session.noArgPredicateMatcher.isAnnotated(this)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> =
        if (classSymbol is FirRegularClassSymbol) {
            setOf(SpecialNames.INIT)
        } else {
            emptySet()
        }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val ownerClass = context.owner
        val ownerClassDeclaredMemberScope = context.declaredScope ?: return emptyList()

        if (!shouldGenerateNoArgConstructor(ownerClass, ownerClassDeclaredMemberScope)) return emptyList()

        val noArgsConstructor = createConstructor(
            owner = ownerClass,
            key = NoArgPluginKey,
            isPrimary = false,
            generateDelegatedNoArgConstructorCall = false
        )

        configureDeprecation(noArgsConstructor)

        return listOf(noArgsConstructor.symbol)
    }

    /**
     * We have to ensure two things with deprecation:
     *
     * Firstly, **the generated constructor is not callable from the Kotlin code.**
     *
     * We use `kotlin.Deprecated(HIDDEN)` to ensure this.
     *
     * Secondly, **the generated constructor remains callable from Java**, since this is
     * the de-facto behavior of the NoArg compiler plugin at the moment (see KT-80633).
     *
     * To achieve this, we mark the constructor with `@java.lang.Deprecated` annotation and rely on the fact
     * that such declarations are not generated as synthetic on the JVM backend (see KT-80649).
     */
    private fun configureDeprecation(noArgsConstructor: FirConstructor) {
        val kotlinDeprecationAnnotation = buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef {
                coneType = StandardClassIds.Annotations.Deprecated.toLookupTag()
                    .constructClassType(typeArguments = ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = false)
            }

            argumentMapping = buildAnnotationArgumentMapping {
                mapping[StandardClassIds.Annotations.ParameterNames.deprecatedMessage] = buildLiteralExpression(
                    source = null,
                    kind = ConstantValueKind.String,
                    value = NO_ARG_CONSTRUCTOR_HIDDEN_DEPRECATED_MESSAGE,
                    setType = true
                )

                mapping[StandardClassIds.Annotations.ParameterNames.deprecatedLevel] = buildEnumEntryDeserializedAccessExpression {
                    enumClassId = StandardClassIds.DeprecationLevel
                    enumEntryName = Name.identifier(DeprecationLevel.HIDDEN.name)
                }
            }
        }

        val isJavaDeprecationAvailable =
            session.symbolProvider.getClassLikeSymbolByClassId(JvmStandardClassIds.Annotations.Java.Deprecated) != null

        val javaDeprecationAnnotation = runIf(isJavaDeprecationAvailable) {
            buildAnnotation {
                annotationTypeRef = buildResolvedTypeRef {
                    coneType = JvmStandardClassIds.Annotations.Java.Deprecated.toLookupTag()
                        .constructClassType(typeArguments = ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = false)
                }

                argumentMapping = buildAnnotationArgumentMapping()
            }
        }

        val annotations = listOfNotNull(
            kotlinDeprecationAnnotation,
            javaDeprecationAnnotation
        )

        noArgsConstructor.replaceAnnotations(annotations)
        noArgsConstructor.replaceDeprecationsProvider(annotations.getDeprecationsProviderFromAnnotations(session, fromJava = false))
    }

    private fun shouldGenerateNoArgConstructor(
        classSymbol: FirClassSymbol<*>,
        classDeclaredMemberScope: FirClassDeclaredMemberScope
    ): Boolean {
        // Only for regular classes
        if (classSymbol !is FirRegularClassSymbol) return false
        if (classSymbol.classKind != ClassKind.CLASS) return false

        // The class must be annotated with a NoArg annotation
        if (!classSymbol.isAnnotatedWithNoArg()) return false

        // Not for inner or local classes
        if (classSymbol.isInner) return false
        if (classSymbol.isLocal) return false

        val declaredConstructors = classDeclaredMemberScope.getDeclaredConstructors()
        if (declaredConstructors.any { it.isZeroParameterConstructor() }) return false

        // Superclass must have a no-arg constructor OR be annotated (and will get one generated)
        val superClassSymbol = classSymbol.getSuperClassSymbolOrAny(session) ?: return true
        val superHasNoArg = superClassSymbol.constructors(session).any { it.isZeroParameterConstructor() }
        val superAnnotated = superClassSymbol.isAnnotatedWithNoArg()
        return superHasNoArg || superAnnotated
    }

    private fun FirConstructorSymbol.isZeroParameterConstructor(): Boolean {
        return valueParameterSymbols.all { it.hasDefaultValue } &&
                (valueParameterSymbols.isEmpty() || isPrimary || hasAnnotation(JvmStandardClassIds.JVM_OVERLOADS_CLASS_ID, session))
    }
}

