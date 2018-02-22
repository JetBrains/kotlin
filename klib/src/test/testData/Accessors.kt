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

package custom.pkg

annotation class A

class Foo {
    val simple = 0

    private val privateSimple = 0

    protected val protectedSimple = 0

    var privateSetter = 0
        private set

    @A val annotated = 0

    val annotatedGetter = 0
        @A get

    var annotatedSetter = 0
        @A set

    var annotatedAccessors = 0
        @A set
        @A get
}
