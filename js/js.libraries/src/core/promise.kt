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

package kotlin.js

/**
 * Exposes the JavaScript [Promise object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Promise) to Kotlin.
 */
public open external class Promise<out T>(executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit) {
    companion object {
        public fun <S> all(promise: Array<out Promise<S>>): Promise<Array<in S>>

        public fun <S> race(promise: Array<out Promise<S>>): Promise<S>

        public fun reject(e: Throwable): Promise<Nothing>

        public fun <S> resolve(e: S): Promise<S>
    }

    public open fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)? = definedExternally): Promise<S>

    public open fun <S> catch(onRejected: (Throwable) -> S): Promise<S>
}