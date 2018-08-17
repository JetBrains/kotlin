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

package kotlin.coroutines.intrinsics

import kotlin.coroutines.*
// TODO: implement them right.

public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
        completion: Continuation<T>
): Any? {
    TODO("Unimplemented")
}

public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        completion: Continuation<T>
): Any? {
    TODO("Unimplemented")
}

@SinceKotlin("1.3")
public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
        completion: Continuation<T>
): Continuation<Unit> {
    TODO("Unimplemented")
}

@SinceKotlin("1.3")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> {
    TODO("Unimplemented")
}

@SinceKotlin("1.3")
public actual fun <T> Continuation<T>.intercepted(): Continuation<T> = TODO("unimplemented")
