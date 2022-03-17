/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*

interface CirTypeVisitor {
    fun visit(type: CirType)
    fun visit(flexibleType: CirFlexibleType)
    fun visit(simpleType: CirSimpleType)
    fun visit(typeParameterType: CirTypeParameterType)
    fun visit(classOrTypeAliasType: CirClassOrTypeAliasType)
    fun visit(classType: CirClassType)
    fun visit(typeAliasType: CirTypeAliasType)
    fun visit(typeProjection: CirTypeProjection)
}

open class BasicCirTypeVisitor : CirTypeVisitor {
    override fun visit(type: CirType) {
        return when (type) {
            is CirFlexibleType -> visit(type)
            is CirSimpleType -> visit(type)
        }
    }

    override fun visit(flexibleType: CirFlexibleType) {
        visit(flexibleType.lowerBound)
        visit(flexibleType.upperBound)
    }

    override fun visit(simpleType: CirSimpleType) {
        when (simpleType) {
            is CirTypeParameterType -> visit(simpleType)
            is CirClassOrTypeAliasType -> visit(simpleType)
        }
    }

    override fun visit(typeParameterType: CirTypeParameterType) {
    }

    override fun visit(classOrTypeAliasType: CirClassOrTypeAliasType) {
        when (classOrTypeAliasType) {
            is CirClassType -> visit(classOrTypeAliasType)
            is CirTypeAliasType -> visit(classOrTypeAliasType)
        }
    }

    override fun visit(classType: CirClassType) {
        classType.outerType?.let { visit(it) }
        classType.arguments.forEach { visit(it) }
    }

    override fun visit(typeAliasType: CirTypeAliasType) {
        visit(typeAliasType.underlyingType)
    }

    override fun visit(typeProjection: CirTypeProjection) {
        if (typeProjection is CirRegularTypeProjection)
            visit(typeProjection.type)
    }
}

fun CirType.accept(visitor: CirTypeVisitor) {
    visitor.visit(this)
}
