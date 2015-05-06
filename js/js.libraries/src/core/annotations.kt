/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

native
public annotation class native(public val name: String = "")
native
public annotation class nativeGetter
native
public annotation class nativeSetter
native
public annotation class nativeInvoke

native
public annotation class library(public val name: String = "")
native
public annotation class enumerable()

// TODO make it "internal" or "fake"
native
deprecated("Do not use this annotation: it is for internal use only")
public annotation class marker