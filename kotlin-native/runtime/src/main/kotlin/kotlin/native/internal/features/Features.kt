/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.features

import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic

@InternalForKotlinNative
@TypedIntrinsic(IntrinsicType.IS_SWIFT_EXPORT_ENABLED)
public external fun isSwiftExportEnabled(): Boolean

@InternalForKotlinNative
@TypedIntrinsic(IntrinsicType.IS_OBJC_INTEROP_ENABLED)
public external fun isObjCInteropEnabled(): Boolean