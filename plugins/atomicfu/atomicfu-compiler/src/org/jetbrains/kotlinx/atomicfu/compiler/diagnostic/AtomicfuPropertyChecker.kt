/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.diagnostic

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInstanceExtension
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.getContainingSymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.text
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicfuStandardClassIds
import org.jetbrains.kotlinx.atomicfu.compiler.diagnostic.AtomicfuErrorMessages.DeclarationInSame

private fun FirProperty.isKotlinxAtomicfu(): Boolean =
    returnTypeRef.coneType.classId?.packageFqName == AtomicfuStandardClassIds.BASE_ATOMICFU_PACKAGE

private val FirProperty.resolvedVisibility: EffectiveVisibility
    get() = publishedApiEffectiveVisibility ?: effectiveVisibility

object AtomicfuPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        checkDelegatedPropertyContainer(declaration)

        // Check extension properties with atomic receiver
        if (declaration.isInstanceExtension) {
            val receiverClassId = declaration.receiverParameter?.symbol?.resolvedType?.classId
            if (receiverClassId?.isAtomicArrayType() == true) {
                reporter.reportOn(declaration.source, AtomicfuErrors.ATOMIC_ARRAY_EXTENSION_PROPERTIES_ARE_FORBIDDEN)
            } else if (receiverClassId?.isAtomicType() == true) {
                if (declaration.getter?.isInline != true || declaration.resolvedVisibility.publicApi) {
                    reporter.reportOn(declaration.source, AtomicfuErrors.ATOMIC_EXTENSION_MUST_BE_NON_PUBLIC_INLINE)
                }
            }
        }
        // Check all other properties having atomic type
        if (!declaration.isKotlinxAtomicfu()) return
        if (declaration.symbol is FirLocalPropertySymbol) {
            reporter.reportOn(declaration.source, AtomicfuErrors.ATOMIC_LOCALS_ARE_FORBIDDEN)
            return
        }
        if (!declaration.resolvedVisibility.privateApi &&
            (declaration.isCompanionBlockMember || declaration.isCompanionExtension)
        ) {
            // Companion block's properties and companion extension properties will be supported in the plugin
            // at the same exact time they become available for our users,
            // and it's a chance to make things right from the beginning and support only private properties.
            reporter.reportOn(declaration.source, AtomicfuErrors.NON_PRIVATE_ATOMIC_COMPANIONS_ARE_FORBIDDEN)
        } else if (!declaration.effectiveVisibility.publicApi && declaration.resolvedVisibility.publicApi) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PUBLISHED_API_ATOMICS_ARE_FORBIDDEN,
                declaration.source.text.toString()
            )
        } else if (declaration.resolvedVisibility.publicApi) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PUBLIC_ATOMICS_ARE_FORBIDDEN,
                declaration.source.text.toString()
            )
        }
        if (declaration.isVar) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.ATOMIC_PROPERTIES_SHOULD_BE_VAL,
                declaration.source.text.toString()
            )
        }
        if (!declaration.hasBackingField) {
            reporter.reportOn(declaration.source, AtomicfuErrors.ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD)
        }
        if (declaration.returnTypeRef.coneType.isMarkedNullable) {
            reporter.reportOn(declaration.source, AtomicfuErrors.NULLABLE_ATOMIC_PROPERTIES_ARE_FORBIDDEN)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDelegatedPropertyContainer(declaration: FirProperty) {
        val delegate = declaration.delegate
        val delegateExpression = if (delegate is FirWrappedDelegateExpression) delegate.expression else delegate
        val delegatePropertyAccess = delegateExpression as? FirPropertyAccessExpression ?: return
        // Check if the property delegate is an expression pointing to another property,
        val delegateProperty = delegatePropertyAccess.calleeReference.toResolvedPropertySymbol() ?: return
        // and check that that other property has an atomic type.
        if (delegateProperty.resolvedReturnType.classId?.isAtomicType() != true) return
        // At this point, we certainly are dealing with a property delegated to an atomic property.
        if (declaration.symbol is FirLocalPropertySymbol) {
            // Explicitly report local properties delegated to atomic properties as forbidden because there is no scenario where they can
            // work. The delegate property can't be local, and if it's not local, it and our local property are definitely in different
            // containers. Even though the both cases produce their own diagnostics, reporting local delegated properties as unsupported
            // is a more targeted diagnostics.
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.LOCALS_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN,
            )
            return
        }

        if (declaration.symbol.isInDifferentContainerThan(delegateProperty)) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.ATOMIC_DELEGATE_MUST_BE_IN_SAME_CONTAINER,
                delegateProperty.inSameContainerMessage(),
            )
            return
        }

        // Companion block members are static: there is no instance receiver to verify.
        // The container check has already established the common containing class.
        if (declaration.isCompanionBlockMember && delegateProperty.isCompanionBlockMember) {
            return
        }

        // The instance receiver must be checked to recognize & report the following pattern:
        // class Holder {
        //     private val state = atomic(0);
        //     internal val currentState by Holder().state
        // }
        val receiver = delegatePropertyAccess.dispatchReceiver?.unwrapSmartcastExpression()
        val expectedOwner = declaration.symbol.getContainingSymbol(context.session)
        val receiverIsAllowed = when (receiver) {
            null -> expectedOwner is FirFileSymbol
            is FirThisReceiverExpression ->
                receiver.calleeReference.boundSymbol == expectedOwner
            // This branch is needed to allow companion object references like ClassName.companionObjectProperty.
            is FirResolvedQualifier ->
                receiver.accessedObjectSymbol == expectedOwner
            else -> false
        }

        if (!receiverIsAllowed) {
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PROPERTY_DELEGATED_TO_ATOMIC_MUST_BE_IN_SAME_INSTANCE,
            )
        }
    }

    context(context: CheckerContext)
    private fun FirPropertySymbol.inSameContainerMessage(): String {
        if (isCompanionBlockMember) return DeclarationInSame.COMPANION_BLOCK

        val containingClass = context.session.firProvider.getContainingClass(this) as? FirClassSymbol<*>
            ?: return DeclarationInSame.FILE
        if (containingClass.isCompanion) return DeclarationInSame.COMPANION_OBJECT
        return DeclarationInSame.type(containingClass.classKind)
    }

    context(context: CheckerContext)
    private fun FirPropertySymbol.isInDifferentContainerThan(other: FirPropertySymbol): Boolean {
        val containingSymbol = this.getContainingSymbol(context.session)
        val otherContainingSymbol = other.getContainingSymbol(context.session)

        if (containingSymbol != null || otherContainingSymbol != null) {
            return containingSymbol != otherContainingSymbol
        }

        return false
    }
}
