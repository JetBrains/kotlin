/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.js.dom.html5

native
public val localStorage: Storage = noImpl

native
public val sessionStorage: Storage = noImpl

native
public trait Storage {
    public val length: Int
            get() = noImpl

    public fun key(index: Int): String? = noImpl

    native("getItem")
    public fun get(key: String): String? = noImpl
    native("setItem")
    public fun set(key: String, value: String) {}

    native("removeItem")
    public fun remove(key: String) {}
    public fun clear() {}
}