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

package org.jetbrains.kotlin.js.backend

import org.jetbrains.kotlin.js.backend.ast.JsLocationWithSource

interface SourceLocationConsumer {
    /**
     * Moves current sourcemap position to the new line
     */
    fun newLine()

    /**
     * Pushes the passed Kotlin source location info to the JS position in the sourcemaps currently being built.
     *
     * After each [pushSourceInfo] call, a corresponding [popSourceInfo] method should be called to properly roll back to the previous
     * (outer) element position.
     *
     * A simple JS expression like `println($number);` should result in the following simplified push-pop call sequence:
     *
     * ```
     * pushSourceInfo()    // -> println($number)
     *   pushSourceInfo()    // -> $number
     *   popSourceInfo()     // <- $number
     * popSourceInfo()     // <- println($number)
     * ```
     */
    fun pushSourceInfo(info: JsLocationWithSource?)

    /**
     * Pushes the new declaration location info into the declaration stack for the next source info pushes.
     *
     * Current declaration location determines how source info pushed through [pushSourceInfo] should be treated. It is required to mark
     * certain whole declaration blocks as ignored in generated source maps.
     *
     * Given example code:
     * ```javascript
     * function a() {
     *     var number = 123;
     *     return function () {
     *         println($number);
     *     };
     * }
     * ```
     *
     * Should lead to the following simplified push-pop call sequence:
     * ```
     * pushDeclarationInfo()   // function a starts
     * pushSourceInfo()        // -> function a ...
     *   pushSourceInfo()      // -> var number = 123;
     *   ...
     *   popSourceInfo()       // <- var number = 123;
     *   pushDeclarationInfo() // anonymous function starts
     *   pushSourceInfo()      // -> function () ...
     *     pushSourceInfo()    // -> println($number)
     *       pushSourceInfo()  // -> $number
     *       popSourceInfo()   // <- $number
     *     popSourceInfo()     // <- println($number)
     *   popSourceInfo()       // <- function () ...
     *   popDeclarationInfo()  // anonymous function ends
     * popSourceInfo()         // <- function a ...
     * popDeclarationInfo()    // function a ends
     * ```
     */
    fun pushDeclarationInfo(info: JsLocationWithSource?)

    /**
     * Pops current source location info from the source stack.
     *
     * See the counterpart [pushSourceInfo] KDoc for details.
     */
    fun popSourceInfo()

    /**
     * Pops current declaration location info from the declaration stack.
     *
     * See the counterpart [pushDeclarationInfo] KDoc for details.
     */
    fun popDeclarationInfo()
}
