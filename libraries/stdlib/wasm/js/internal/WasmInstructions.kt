/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:ExcludedFromCodegen
@file:Suppress("unused", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY", "INLINE_CLASS_IN_EXTERNAL_DECLARATION", "UNUSED_PARAMETER")


package kotlin.wasm.internal

import kotlin.wasm.internal.ExternalInterfaceType

// Reference type operators

@WasmOp(WasmOp.REF_IS_NULL)
internal external fun wasm_externref_is_null(a: ExternalInterfaceType?): Boolean
