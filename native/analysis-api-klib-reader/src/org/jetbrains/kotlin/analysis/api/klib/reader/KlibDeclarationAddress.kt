/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

public sealed class KlibDeclarationAddress {
    public abstract val sourceFileName: String?
    public abstract val packageFqName: FqName
}

public sealed class KlibClassifierAddress : KlibDeclarationAddress() {
    public abstract val classId: ClassId
}

public data class KlibClassAddress(
    public override val sourceFileName: String?,
    public override val packageFqName: FqName,
    public override val classId: ClassId,
) : KlibClassifierAddress()

public data class KlibTypeAliasAddress(
    override val packageFqName: FqName,
    override val classId: ClassId,
) : KlibClassifierAddress() {
    /**
     * TypeAlias do not encode their source file name into klibs. This value is always null.
     */
    override val sourceFileName: Nothing? = null
}

public sealed class KlibCallableAddress : KlibDeclarationAddress() {
    public abstract val callableName: Name
}

public data class KlibPropertyAddress(
    override val sourceFileName: String?,
    override val packageFqName: FqName,
    override val callableName: Name,
) : KlibCallableAddress()

public data class KlibFunctionAddress(
    override val sourceFileName: String?,
    override val packageFqName: FqName,
    override val callableName: Name,
) : KlibCallableAddress()
