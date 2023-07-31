/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
/*
* Compiler generates inplace next code:
* ```
* block (result anyref) {
*     local.get 0
*     extern.internalize
*     br_on_cast_fail 0000_0011b 0 (type any) (type $kotlin.Any)
*     return
* }
* ```
*/
internal fun returnArgumentIfItIsKotlinAny(): Unit = implementedAsIntrinsic