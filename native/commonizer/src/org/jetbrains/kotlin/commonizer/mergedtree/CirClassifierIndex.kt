/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.tree.*
import org.jetbrains.kotlin.commonizer.utils.compact
import org.jetbrains.kotlin.commonizer.utils.compactMapValues

internal fun CirClassifierIndex(tree: CirTreeRoot): CirClassifierIndex {
    return CirUnderlyingTypeIndexBuilder().apply { invoke(tree) }.build()
}

internal interface CirClassifierIndex {
    val allClassifierIds: Set<CirEntityId>
    fun findTypeAliasesWithUnderlyingType(underlyingClassifier: CirEntityId): List<CirTreeTypeAlias>
}

private class CirClassifierIndexImpl(
    override val allClassifierIds: Set<CirEntityId>,
    private val typeAliasesByUnderlyingType: Map<CirEntityId, List<CirTreeTypeAlias>>
) : CirClassifierIndex {

    override fun findTypeAliasesWithUnderlyingType(underlyingClassifier: CirEntityId): List<CirTreeTypeAlias> {
        return typeAliasesByUnderlyingType[underlyingClassifier].orEmpty()
    }
}

private class CirUnderlyingTypeIndexBuilder {
    private val index = mutableMapOf<CirEntityId, MutableList<CirTreeTypeAlias>>()
    private val classifierIds = mutableSetOf<CirEntityId>()

    operator fun invoke(tree: CirTreeRoot) {
        tree.modules.forEach { module -> this(module) }
    }

    operator fun invoke(module: CirTreeModule) {
        module.packages.forEach { pkg -> this(pkg) }
    }

    operator fun invoke(pkg: CirTreePackage) {
        pkg.typeAliases.forEach { typeAlias -> this(typeAlias) }
        pkg.classes.forEach { clazz -> this(clazz) }
    }

    operator fun invoke(typeAlias: CirTreeTypeAlias) {
        classifierIds.add(typeAlias.id)
        index.getOrPut(typeAlias.typeAlias.underlyingType.classifierId) { mutableListOf() }.add(typeAlias)
    }

    operator fun invoke(clazz: CirTreeClass) {
        classifierIds.add(clazz.id)
        clazz.classes.forEach { innerClazz -> this(innerClazz) }
    }

    fun build(): CirClassifierIndex {
        return CirClassifierIndexImpl(
            allClassifierIds = classifierIds.toSet(),
            typeAliasesByUnderlyingType = index.compactMapValues { (_, list) -> list.compact() }
        )
    }
}
