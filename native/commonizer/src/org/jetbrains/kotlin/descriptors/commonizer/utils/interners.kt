/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun FqName.intern(): FqName = fqNameInterner.intern(this)
internal fun Name.intern(): Name = nameInterner.intern(this)

@Suppress("NOTHING_TO_INLINE")
internal inline fun internedClassId(topLevelFqName: FqName): ClassId {
    val packageFqName = topLevelFqName.parent().intern()
    val className = topLevelFqName.shortName().intern()
    return internedClassId(packageFqName, className)
}

internal fun internedClassId(packageFqName: FqName, classifierName: Name): ClassId {
    val relativeClassName = FqName.topLevel(classifierName).intern()
    return ClassId(packageFqName, relativeClassName, false).intern()
}

internal fun internedClassId(ownerClassId: ClassId, nestedClassName: Name): ClassId {
    val relativeClassName = ownerClassId.relativeClassName.child(nestedClassName).intern()
    return ClassId(ownerClassId.packageFqName, relativeClassName, ownerClassId.isLocal).intern()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun ClassId.intern(): ClassId = classIdInterner.intern(this)

private val fqNameInterner = Interner<FqName>()
private val nameInterner = Interner<Name>()
private val classIdInterner = Interner<ClassId>()
