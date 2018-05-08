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

package org.jetbrains.kotlin.script.util

import org.junit.Assert
import org.junit.Test

class ScriptUtilContextTests {

    @Test
    fun testExplicitScriptClasspath() {
        withSysProp(KOTLIN_SCRIPT_CLASSPATH_PROPERTY, "a:b:c") {
            val classpath = scriptCompilationClasspathFromContextOrStlib(wholeClasspath = true)
            Assert.assertEquals("a:b:c", classpath.joinToString(":"))
        }
    }

    // TODO: more tests
}

private fun withSysProp(name: String, value: String, body: () -> Unit) {
    val savedValue = System.setProperty(name, value)
    try {
        body()
    }
    finally {
        if (savedValue != null) System.setProperty(name, savedValue)
        else System.clearProperty(name)
    }
}