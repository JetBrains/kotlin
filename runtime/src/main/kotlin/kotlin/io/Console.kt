/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.io

@SymbolName("Kotlin_io_Console_print")
external public fun print(message: String)

/* TODO: use something like that.
public fun<T> print(message: T) {
    print(message.toString())
} */

public actual fun print(message: Any?) {
    print(message.toString())
}

public fun print(message: Byte) {
    print(message.toString())
}

public fun print(message: Short) {
    print(message.toString())
}

public fun print(message: Char) {
    print(message.toString())
}

public fun print(message: Int) {
    print(message.toString())
}

public fun print(message: Long) {
    print(message.toString())
}

public fun print(message: Float) {
    print(message.toString())
}

public fun print(message: Double) {
    print(message.toString())
}

public fun print(message: Boolean) {
    print(message.toString())
}

@SymbolName("Kotlin_io_Console_println")
external public fun println(message: String)

public actual fun println(message: Any?) {
    println(message.toString())
}

public fun println(message: Byte) {
    println(message.toString())
}

public fun println(message: Short) {
    println(message.toString())
}

public fun println(message: Char) {
    println(message.toString())
}

public fun println(message: Int) {
    println(message.toString())
}

public fun println(message: Long) {
    println(message.toString())
}

public fun println(message: Float) {
    println(message.toString())
}

public fun println(message: Double) {
    println(message.toString())
}

public fun println(message: Boolean) {
    println(message.toString())
}
/* TODO: use something like that.
public fun<T> println(message: T) {
    print(message.toString())
} */

@SymbolName("Kotlin_io_Console_println0")
external public actual fun println()

@SymbolName("Kotlin_io_Console_readLine")
external public fun readLine(): String?
