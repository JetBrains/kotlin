/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

@TypedIntrinsic(IntrinsicType.ENUM_VALUES)
@PublishedApi
internal external fun <T : Enum<T>> enumValuesIntrinsic(): Array<T>

@TypedIntrinsic(IntrinsicType.ENUM_VALUE_OF)
@PublishedApi
internal external fun <T : Enum<T>> enumValueOfIntrinsic(name: String): T
