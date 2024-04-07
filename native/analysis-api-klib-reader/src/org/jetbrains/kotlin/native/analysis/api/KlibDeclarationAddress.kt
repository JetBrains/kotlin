/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.nio.file.Path

public sealed class KlibDeclarationAddress {
    internal abstract val libraryPath: Path
    public abstract val sourceFileName: String?
    public abstract val packageFqName: FqName
}

public sealed class KlibClassifierAddress : KlibDeclarationAddress() {
    public abstract val classId: ClassId
}

// todo: replace suppress with @ConsistentCopyVisibility annotation after bootstrap
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class KlibClassAddress internal constructor(
    override val libraryPath: Path,
    public override val sourceFileName: String?,
    public override val packageFqName: FqName,
    public override val classId: ClassId,
) : KlibClassifierAddress()

// todo: replace suppress with @ConsistentCopyVisibility annotation after bootstrap
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class KlibTypeAliasAddress internal constructor(
    override val libraryPath: Path,
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

// todo: replace suppress with @ConsistentCopyVisibility annotation after bootstrap
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class KlibPropertyAddress internal constructor(
    override val libraryPath: Path,
    override val sourceFileName: String?,
    override val packageFqName: FqName,
    override val callableName: Name,
) : KlibCallableAddress()

// todo: replace suppress with @ConsistentCopyVisibility annotation after bootstrap
@Suppress("DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING")
public data class KlibFunctionAddress internal constructor(
    override val libraryPath: Path,
    override val sourceFileName: String?,
    override val packageFqName: FqName,
    override val callableName: Name,
) : KlibCallableAddress()

