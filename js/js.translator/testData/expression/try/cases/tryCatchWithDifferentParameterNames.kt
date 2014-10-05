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

package foo

fun bar(e: Exception): String {
    var s: String = ""
    var exceptionObject: Exception? = null

    try {
        throw e
    }
    catch (e1: IllegalArgumentException) {
        s = "IllegalArgumentException"
        exceptionObject = e1
    }
    catch (e2: Exception) {
        s = "Exception"
        exceptionObject = e
    }

    assertEquals(e, exceptionObject, "e == exceptionObject")
    return s
}

fun box(): String {

    assertEquals("IllegalArgumentException", bar(IllegalArgumentException()))
    assertEquals("Exception", bar(Exception()))

    return "OK"
}
