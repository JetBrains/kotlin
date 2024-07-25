/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

enum class MemberEmbeddingPolicy {
    PRIVATE,
    PUBLIC,
    UNSCOPED,
}

fun alwaysScopedPolicy(isPrivate: Boolean): MemberEmbeddingPolicy =
    if (isPrivate) MemberEmbeddingPolicy.PRIVATE else MemberEmbeddingPolicy.PUBLIC

fun onlyPrivateScopedPolicy(isPrivate: Boolean): MemberEmbeddingPolicy =
    if (isPrivate) MemberEmbeddingPolicy.PRIVATE else MemberEmbeddingPolicy.UNSCOPED

val MemberEmbeddingPolicy.isScoped: Boolean
    get() = this == MemberEmbeddingPolicy.PRIVATE || this == MemberEmbeddingPolicy.PUBLIC