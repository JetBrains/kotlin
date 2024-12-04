/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION_ERROR", "TYPEALIAS_EXPANSION_DEPRECATION_ERROR", "UNUSED_PARAMETER", "NO_EXPLICIT_VISIBILITY_IN_API_MODE", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE") // Everything is scheduled for removal
@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.wasm.jsinterop

import kotlin.native.*
import kotlin.native.internal.*
import kotlinx.cinterop.*

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
typealias Arena = Int
@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
typealias Object = Int
@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
typealias Pointer = Int

private const val WASM_TARGET_IS_DEPRECATED = "K/N WASM target was removed and all related API is deprecated for removal. " +
        "See https://blog.jetbrains.com/kotlin/2023/02/update-regarding-kotlin-native-targets for additional details"

/**
 * @Retain annotation is required to preserve functions from internalization and DCE.
 */
@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun allocateArena(): Arena = error(WASM_TARGET_IS_DEPRECATED)

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun freeArena(arena: Arena) { error(WASM_TARGET_IS_DEPRECATED) }

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun pushIntToArena(arena: Arena, value: Int) { error(WASM_TARGET_IS_DEPRECATED) }

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
const val upperWord = 0xffffffff.toLong() shl 32

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun doubleUpper(value: Double): Int =
    ((value.toBits() and upperWord) ushr 32) .toInt()

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun doubleLower(value: Double): Int =
    (value.toBits() and 0x00000000ffffffff) .toInt()

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun ReturnSlot_getDouble(): Double = error(WASM_TARGET_IS_DEPRECATED)

@GCUnsafeCall("Kotlin_String_utf16pointer")
@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
external public fun stringPointer(message: String): Pointer

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
@GCUnsafeCall("Kotlin_String_utf16length")
external public fun stringLengthBytes(message: String): Int

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
typealias KtFunction <R> = ((ArrayList<JsValue>)->R)

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun <R> wrapFunction(func: KtFunction<R>): Int {
    val ptr: Long = StableRef.create(func).asCPointer().toLong() 
    return ptr.toInt() // TODO: LP64 unsafe.
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun runLambda(pointer: Int, argumentsArena: Arena, argumentsArenaSize: Int): Int {
    val arguments = arrayListOf<JsValue>()
    for (i in 0 until argumentsArenaSize) {
        arguments.add(JsValue(argumentsArena, i));
    }
    val previousArena = ArenaManager.currentArena
    ArenaManager.currentArena = argumentsArena
    // TODO: LP64 unsafe: wasm32 passes Int, not Long.
    val func = pointer.toLong().toCPointer<CPointed>()!!.asStableRef<KtFunction<JsValue>>().get()
    val result = func(arguments)

    ArenaManager.currentArena = previousArena
    return result.index
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
open class JsValue(val arena: Arena, val index: Object) {
    fun getInt(property: String): Int {
        return getInt(ArenaManager.currentArena, index, stringPointer(property), stringLengthBytes(property))
    }
    fun getProperty(property: String): JsValue {
        return JsValue(ArenaManager.currentArena, Konan_js_getProperty(ArenaManager.currentArena, index, stringPointer(property), stringLengthBytes(property)))
    }
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
open class JsArray(arena: Arena, index: Object): JsValue(arena, index) {
    constructor(jsValue: JsValue): this(jsValue.arena, jsValue.index)        
    operator fun get(index: Int): JsValue {
        // TODO: we could pass an integer index to index arrays.
        return getProperty(index.toString())
    }
    val size: Int
        get() = this.getInt("length")
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun getInt(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int = error(WASM_TARGET_IS_DEPRECATED)

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun Konan_js_getProperty(arena: Arena, obj: Object, propertyPtr: Pointer, propertyLen: Int): Int = error(WASM_TARGET_IS_DEPRECATED)

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun setFunction(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int , function: Int) { error(WASM_TARGET_IS_DEPRECATED) }

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
public fun setString(arena: Arena, obj: Object, propertyName: Pointer, propertyLength: Int, stringPtr: Pointer, stringLength: Int ) { error(WASM_TARGET_IS_DEPRECATED) }

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun setter(obj: JsValue, property: String, string: String) {
    setString(obj.arena, obj.index, stringPointer(property), stringLengthBytes(property), stringPointer(string), stringLengthBytes(string))
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun setter(obj: JsValue, property: String, lambda: KtFunction<Unit>) {
    val pointer = wrapFunction(lambda);
    setFunction(obj.arena, obj.index, stringPointer(property), stringLengthBytes(property), pointer)
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun JsValue.setter(property: String, lambda: KtFunction<Unit>) {
    setter(this, property, lambda)
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
fun JsValue.setter(property: String, string: String) {
    setter(this, property, string)
}

@Deprecated(WASM_TARGET_IS_DEPRECATED, level = DeprecationLevel.ERROR)
object ArenaManager {
    val globalArena = allocateArena()

    @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
    var currentArena = globalArena
}
