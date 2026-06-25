/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package objcCache.interfaceImpl

class EnglishGreeter : Greeter {
    override fun greet(name: String): String = "Hello, $name!"
}

fun createGreeter(): Greeter = EnglishGreeter()
