/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun FqName.child(child: FqName): FqName =
    child.pathSegments().fold(this) { accumulator, segment -> accumulator.child(segment) }

internal fun List<Name>.fqNameBeforeIndex(toIndexExclusive: Int): FqName =
    if (toIndexExclusive == 0) FqName.ROOT else FqName(subList(0, toIndexExclusive).joinToString("."))

internal fun FqName.removeSuffix(suffix: FqName): FqName {
    val pathSegments = pathSegments()
    val suffixPathSegments = suffix.pathSegments()

    val suffixStart = pathSegments.size - suffixPathSegments.size
    assertEquals(suffixPathSegments, pathSegments.subList(suffixStart, pathSegments.size))

    return FqName(pathSegments.take(suffixStart).joinToString("."))
}

internal fun KtDotQualifiedExpression.collectNames(): List<Name> {
    val output = mutableListOf<Name>()

    fun KtExpression.recurse(): Boolean {
        children.forEach { child ->
            when (child) {
                is KtExpression -> when (child) {
                    is KtDotQualifiedExpression -> if (!child.recurse()) return false
                    is KtCallExpression,
                    is KtArrayAccessExpression,
                    is KtClassLiteralExpression,
                    is KtPostfixExpression -> {
                        child.recurse()
                        return false
                    }
                    is KtCallableReferenceExpression -> {
                        // 'T' from 'T::foo' should be considered, '::foo' should be discarded.
                        child.getChildrenOfType<KtNameReferenceExpression>()
                            .takeIf { it.size == 2 }
                            ?.first()
                            ?.let { output += it.getReferencedNameAsName() }
                        return false
                    }
                    is KtSafeQualifiedExpression -> {
                        // Consider only the first KtNameReferenceExpression child.
                        output.addIfNotNull(child.getChildOfType<KtNameReferenceExpression>()?.getReferencedNameAsName())
                        return false
                    }
                    is KtNameReferenceExpression -> output += child.getReferencedNameAsName()
                    else -> return false
                }
                else -> return false
            }
        }
        return true
    }

    recurse()
    return output
}

internal fun KtUserType.collectNames(output: MutableList<Name> = mutableListOf()): List<Name> {
    children.forEach { child ->
        when (child) {
            is KtUserType -> child.collectNames(output)
            is KtNameReferenceExpression -> output += child.getReferencedNameAsName()
            else -> Unit
        }
    }

    return output
}

internal fun KtElement.collectAccessibleDeclarationNames(): Set<Name> {
    val names = hashSetOf<Name>()

    if (this is KtTypeParameterListOwner) {
        typeParameters.mapTo(names) { it.nameAsSafeName }
    }

    children.forEach { child ->
        if (child is KtNamedDeclaration) {
            when (child) {
                is KtClassLikeDeclaration,
                is KtVariableDeclaration,
                is KtParameter,
                is KtTypeParameter -> names += child.nameAsSafeName
            }
        }

        if (child is KtDestructuringDeclaration) {
            child.entries.mapTo(names) { it.nameAsSafeName }
        }

    }

    return names
}
