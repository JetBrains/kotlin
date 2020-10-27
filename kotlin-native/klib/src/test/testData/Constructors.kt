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

@file:Suppress("UNUSED_PARAMETER")

annotation class A

class Foo(x: Int) {
    constructor(): this(0)
    constructor(x: Double): this(x.toInt())
    constructor(x: Double, y: Int): this(y)

    private constructor(x: Long): this(0)
    protected constructor(x: String): this(0)
    @A constructor(x: Foo) : this(0)
}

class Bar @A constructor(x: Int)
class Baz private constructor(x: Int)
class Qux protected constructor(x: Int)

class Typed<T>(x: Int) {
    constructor(): this(0)
    constructor(x: Double): this(x.toInt())
    constructor(x: Double, y: Int): this(y)

    private constructor(x: Long): this(0)
    protected constructor(x: String): this(0)
    @A constructor(x: Foo) : this(0)
}
