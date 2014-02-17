/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package foo

fun run<T>(f: () -> T) = f()

val r = "OK"

fun simple(s: String? = null): String {
    if (s != null) return s

    return run {
        simple("OK")
    }
}

val ok = "OK"
fun withClosure(s: String? = null): String {
    if (s != null) return s

    return ok + run {
        withClosure(ok)
    }
}

fun box(): String {
    if (simple("OK") != "OK") return "failed on simple recursion"

    if (withClosure() != ok + ok) return "failed when closure something"

    return "OK"
}
