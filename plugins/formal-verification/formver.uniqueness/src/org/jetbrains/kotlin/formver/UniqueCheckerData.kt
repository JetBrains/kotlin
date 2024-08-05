/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class UniqueCheckerData(
    override val session: FirSession,
    override val config: PluginConfiguration,
    override val errorCollector: ErrorCollector,
    override val uniqueStack: ArrayDeque<ArrayDeque<PathUnique>> = ArrayDeque(),
) : UniqueCheckerContext {

    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    override val uniqueId: ClassId
        get() = getAnnotationId("Unique")
    private val sharedId: ClassId
        get() = getAnnotationId("Shared")
    private val borrowedId: ClassId
        get() = getAnnotationId("Borrowed")

    override fun resolveUniqueAnnotation(declaration: FirDeclaration): UniqueLevel {
        return if (declaration.hasAnnotation(borrowedId, session) && declaration.hasAnnotation(sharedId, session)) {
            UniqueLevel.BorrowedShared
        } else if (declaration.hasAnnotation(borrowedId, session) && declaration.hasAnnotation(uniqueId, session)) {
            UniqueLevel.BorrowedUnique
        } else if (declaration.hasAnnotation(sharedId, session)) {
            UniqueLevel.Shared
        } else if (declaration.hasAnnotation(uniqueId, session)) {
            UniqueLevel.Unique
        } else UniqueLevel.Shared
    }
}