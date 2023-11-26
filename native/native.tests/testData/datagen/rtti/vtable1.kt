/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

abstract class Super {
    abstract fun bar()
}

class Foo : Super() {
    final override fun bar() {}
}

fun box(): String {
    // This test now checks that the source can be successfully compiled and linked;
    // TODO: check the contents of TypeInfo?
    return "OK"
}
