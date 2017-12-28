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

package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.cli.bc.main as konancMain
import org.jetbrains.kotlin.cli.klib.main as klibMain

fun main(args: Array<String>) {
    val utilityName = args[0]
    val utilityArgs = args.drop(1).toTypedArray()
    when (utilityName) {
        "konanc" ->
            konancMain(utilityArgs)
        "cinterop" -> {
            val konancArgs = invokeInterop("native", utilityArgs)
            konancMain(konancArgs)
        }
        "jsinterop" -> {
            val konancArgs = invokeInterop("wasm", utilityArgs)
            konancMain(konancArgs)
        }
        "klib" ->
            klibMain(utilityArgs)
        else ->
            error("Unexpected utility name")
    }
}

