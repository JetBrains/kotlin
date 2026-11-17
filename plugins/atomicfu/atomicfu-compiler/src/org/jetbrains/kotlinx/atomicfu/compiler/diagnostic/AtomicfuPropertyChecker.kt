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
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.resolvedType
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
        checkDelegatedProperty(declaration)

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
    private fun checkDelegatedProperty(declaration: FirProperty) {
        val delegate = declaration.delegate
        val delegateExpression = (if (delegate is FirWrappedDelegateExpression) delegate.expression else delegate)
            ?.unwrapSmartcastExpression()
        // Check if the delegate expression (whatever it is) has an atomic type.
        if (delegateExpression?.resolvedType?.classId?.isAtomicType() != true) return

        // At this point, we certainly are dealing with a property delegated to an atomic.
        if (declaration.symbol is FirLocalPropertySymbol) {
            // Local properties delegated to atomics can never be compiled, whatever the delegate is:
            // - a local atomic property: it is itself reported with ATOMIC_LOCALS_ARE_FORBIDDEN;
            // - a non-local atomic property: it is necessarily in a different container than the local property;
            // - an atomic factory call (`by atomic(0)`): the backend reports ATOMIC_LOCALS_ARE_FORBIDDEN on it.
            // Report a dedicated diagnostic in the frontend, which is more targeted than the errors above
            // and surfaces the problem without having to run the backend.
            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.LOCAL_PROPERTIES_DELEGATED_TO_ATOMICS_ARE_FORBIDDEN,
            )
            return
        }

        // Check if the property delegate is an expression pointing to another property.
        val delegatePropertyAccess = delegateExpression as? FirPropertyAccessExpression ?: return
        val delegateProperty = delegatePropertyAccess.calleeReference.toResolvedPropertySymbol() ?: return

        val declarationOwner = declaration.symbol.getContainingSymbol(context.session)
        // Check if the property and its delegate are in the same container.
        if (declarationOwner != delegateProperty.getContainingSymbol(context.session)) {
            if (delegateProperty is FirLocalPropertySymbol) {
                // This checker reports an `ATOMIC_LOCALS_ARE_FORBIDDEN` error at the delegate property
                // because it's local, highlighting a high-priority issue for the user to deal with.
                // Any diagnostics on top of that on the delegated property itself might be confusing.
                return
            }

            reporter.reportOn(
                declaration.source,
                AtomicfuErrors.PROPERTIES_DELEGATED_TO_ATOMICS_MUST_BE_IN_SAME_CONTAINER,
                delegateProperty.name.asString(),
                delegateProperty.inSameContainerMessage(),
            )
            return
        }

        // Companion block members are static: there is no instance receiver to verify.
        // The container check has already established the common containing class.
        if (declaration.isCompanionBlockMember && delegateProperty.isCompanionBlockMember) {
            return
        }

        if (!delegatePropertyAccess.accessesOwnInstance(declarationOwner)) {
            if (declaration.isCompanionBlockMember) {
                reporter.reportOn(
                    declaration.source,
                    AtomicfuErrors.COMPANION_PROPERTIES_CAN_ONLY_DELEGATE_TO_ATOMICS_IN_COMPANION_BLOCK,
                )
            } else {
                reporter.reportOn(
                    declaration.source,
                    AtomicfuErrors.INSTANCE_PROPERTIES_DELEGATED_TO_ATOMICS_CAN_ONLY_ACCESS_SAME_INSTANCE,
                    delegateProperty.name.asString()
                )
            }
        }
    }

    context(context: CheckerContext)
    private fun FirPropertySymbol.inSameContainerMessage(): String {
        if (isCompanionBlockMember) return DeclarationInSame.COMPANION_BLOCK

        val containingClass = getContainingSymbol(context.session) as? FirClassSymbol<*>
            ?: return DeclarationInSame.FILE
        if (containingClass.isCompanion) return DeclarationInSame.COMPANION_OBJECT
        return DeclarationInSame.type(containingClass.classKind)
    }

    /**
     * Checks whether this access to an atomic property goes through the instance owned by [expectedOwner], that is,
     * its dispatch receiver is one of:
     * - none, for top-level properties (the owner is a file);
     * - implicit or explicit `this` (including `this@ClassName`) bound to [expectedOwner];
     * - a qualifier referring to the [expectedOwner] object, like `ClassName.companionProperty` or `Object.objectProperty`.
     *
     * Any other receiver, e.g., `Holder().state` inside `Holder`, refers to a different instance and is rejected.
     *
     * Relies on the same-container check having passed: none of the branches above verifies that the atomic property
     * itself is declared in [expectedOwner] (it could be inherited, or declared in another file).
     */
    private fun FirPropertyAccessExpression.accessesOwnInstance(expectedOwner: FirBasedSymbol<*>?): Boolean {
        return when (val receiver = dispatchReceiver?.unwrapSmartcastExpression()) {
            null -> expectedOwner is FirFileSymbol
            is FirThisReceiverExpression ->
                receiver.calleeReference.boundSymbol == expectedOwner
            // This branch is needed to allow object references like `by ClassName.companionObjectProperty` or `by Object.objectProperty`.
            is FirResolvedQualifier ->
                receiver.accessedObjectSymbol == expectedOwner
            else -> false
        }
    }
}
