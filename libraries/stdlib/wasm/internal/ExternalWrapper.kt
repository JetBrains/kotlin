/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
/*
* Compiler generates inplace next code, expecting externref as the first function parameter:
* ```
* block (result anyref) {
*     local.get 0
*     extern.internalize
*     br_on_cast_fail 0000_0011b 0 (type any) (type $kotlin.Any)
*     return
* }
* ```
* In `-Xwasm-use-shared-objects` mode emits no code, as non-shared `externref`s cannot represent shared Kotlin objects.
*/
internal fun returnArgumentIfItIsKotlinAny(): Unit = implementedAsIntrinsic

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
/*
 * Same as above, but expects shareable externref as the first parameter, and thus in `-Xwasm-use-shared-objects` mode
 * emits similar code with shared types: `block (result (ref null (shared any)))`
 */
internal fun returnShareableArgumentIfItIsKotlinAny(): Unit = implementedAsIntrinsic

@Target(AnnotationTarget.FUNCTION)
internal annotation class JsBuiltin(
    val module: String,
    val name: String = "",
    val polyfill: String = ""
)
