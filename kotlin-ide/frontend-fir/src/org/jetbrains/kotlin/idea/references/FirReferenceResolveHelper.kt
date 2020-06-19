/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.calls.SyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

object FirReferenceResolveHelper {
    fun FirResolvedTypeRef.toTargetPsi(session: FirSession): PsiElement? {
        val type = type as? ConeLookupTagBasedType ?: return null
        return (type.lookupTag.toSymbol(session) as? AbstractFirBasedSymbol<*>)?.fir?.findPsi(session)
    }

    fun ClassId.toTargetPsi(session: FirSession, calleeReference: FirReference? = null): PsiElement? {
        val classLikeDeclaration = ConeClassLikeLookupTagImpl(this).toSymbol(session)?.fir
        if (classLikeDeclaration is FirRegularClass) {
            if (calleeReference is FirResolvedNamedReference) {
                val callee = calleeReference.resolvedSymbol.fir as? FirCallableMemberDeclaration
                // TODO: check callee owner directly?
                if (callee !is FirConstructor && callee?.isStatic != true) {
                    classLikeDeclaration.companionObject?.let { return it.findPsi(session) }
                }
            }
        }
        return classLikeDeclaration?.findPsi(session)
    }

    fun FirReference.toTargetPsi(session: FirSession): PsiElement? {
        return when (this) {
            is FirResolvedNamedReference -> {
                val targetFir = when (val symbol = resolvedSymbol) {
                    is SyntheticPropertySymbol -> {
                        val syntheticProperty = symbol.fir as FirSyntheticProperty
                        if (syntheticProperty.getter.delegate.symbol.callableId == symbol.accessorId) {
                            syntheticProperty.getter.delegate
                        } else {
                            syntheticProperty.setter!!.delegate
                        }
                    }
                    else -> symbol.fir
                }
                targetFir.findPsi(session)
            }
            is FirResolvedCallableReference -> {
                resolvedSymbol.fir.findPsi(session)
            }
            is FirThisReference -> {
                boundSymbol?.fir?.findPsi(session)
            }
            is FirSuperReference -> {
                (superTypeRef as? FirResolvedTypeRef)?.toTargetPsi(session)
            }
            else -> {
                null
            }
        }
    }

    internal fun resolveSimpleNameReference(ref: KtSimpleNameReferenceFirImpl): Collection<PsiElement> {
        val expression = ref.expression
        val fir = expression.getOrBuildFir()
        val session = expression.session
        when (fir) {
            is FirResolvable -> {
                val calleeReference =
                    if (fir is FirFunctionCall
                        && fir.isImplicitFunctionCall()
                        && expression is KtNameReferenceExpression
                    ) {
                        // we are resolving implicit invoke call, like
                        // fun foo(a: () -> Unit) {
                        //     <expression>a</expression>()
                        // }
                        (fir.dispatchReceiver as FirQualifiedAccessExpression).calleeReference
                    } else fir.calleeReference
                return listOfNotNull(calleeReference.toTargetPsi(session))
            }
            is FirResolvedTypeRef -> {
                return listOfNotNull(fir.toTargetPsi(session))
            }
            is FirResolvedQualifier -> {
                val classId = fir.classId ?: return emptyList()
                // Distinguish A.foo() from A(.Companion).foo()
                // Make expression.parent as? KtDotQualifiedExpression local function
                var parent = expression.parent as? KtDotQualifiedExpression
                while (parent != null) {
                    val selectorExpression = parent.selectorExpression ?: break
                    if (selectorExpression === expression) {
                        parent = parent.parent as? KtDotQualifiedExpression
                        continue
                    }
                    val parentFir = selectorExpression.getOrBuildFir()
                    if (parentFir is FirQualifiedAccess) {
                        return listOfNotNull(classId.toTargetPsi(session, parentFir.calleeReference))
                    }
                    parent = parent.parent as? KtDotQualifiedExpression
                }
                return listOfNotNull(classId.toTargetPsi(session))
            }
            is FirAnnotationCall -> {
                val type = fir.typeRef as? FirResolvedTypeRef ?: return emptyList()
                return listOfNotNull(type.toTargetPsi(session))
            }
            is FirResolvedImport -> {
                var parent = expression.parent
                while (parent is KtDotQualifiedExpression) {
                    if (parent.selectorExpression !== expression) {
                        // Special: package reference in the middle of import directive
                        // import a.<caret>b.c.SomeClass
                        // TODO: return reference to PsiPackage
                        return listOf(expression)
                    }
                    parent = parent.parent
                }
                val classId = fir.resolvedClassId
                if (classId != null) {
                    return listOfNotNull(classId.toTargetPsi(session))
                }
                val name = fir.importedName ?: return emptyList()
                val symbolProvider = session.firSymbolProvider
                return symbolProvider.getTopLevelCallableSymbols(fir.packageFqName, name)
                    .mapNotNull { it.fir.findPsi(session) } +
                        listOfNotNull(
                            symbolProvider
                                .getClassLikeSymbolByFqName(ClassId(fir.packageFqName, name))
                                ?.fir?.findPsi(session)
                        )
            }
            is FirFile -> {
                if (expression.getNonStrictParentOfType<KtPackageDirective>() != null) {
                    // Special: package reference in the middle of package directive
                    return listOf(expression)
                }
                return listOfNotNull(fir.findPsi(session))
            }
            is FirArrayOfCall -> {
                // We can't yet find PsiElement for arrayOf, intArrayOf, etc.
                return emptyList()
            }
            is FirReturnExpression -> {
                return if (expression is KtLabelReferenceExpression) {
                    listOfNotNull(fir.target.labeledElement.findPsi(session))
                } else emptyList()
            }
            is FirErrorNamedReference -> {
                return emptyList()
            }
            else -> {
                // Handle situation when we're in the middle/beginning of qualifier
                // <caret>A.B.C.foo() or A.<caret>B.C.foo()
                // NB: in this case we get some parent FIR, like FirBlock, FirProperty, FirFunction or the like
                var parent = expression.parent as? KtDotQualifiedExpression
                var unresolvedCounter = 1
                while (parent != null) {
                    val selectorExpression = parent.selectorExpression ?: break
                    if (selectorExpression === expression) {
                        parent = parent.parent as? KtDotQualifiedExpression
                        continue
                    }
                    val parentFir = selectorExpression.getOrBuildFir()
                    if (parentFir is FirResolvedQualifier) {
                        var classId = parentFir.classId
                        while (unresolvedCounter > 0) {
                            unresolvedCounter--
                            classId = classId?.outerClassId
                        }
                        return listOfNotNull(classId?.toTargetPsi(session))
                    }
                    parent = parent.parent as? KtDotQualifiedExpression
                    unresolvedCounter++
                }
                return emptyList()
            }
        }
    }
}
