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

package org.jetbrains.kotlin.gradle

import android.app.Application
import android.test.ApplicationTestCase

class InternalDummyApplicationTest : ApplicationTestCase<Application>(Application::class.java) {
    init {
        val dummy = InternalDummy("World")
        assert("Hello World!" == dummy.greeting) { "Expected: 'Hello World!'. Actual value: ${dummy.greeting}" }

        // Check that the Java sources from the tested variant are available
        val bar = foo.FooJavaClass()
    }
}