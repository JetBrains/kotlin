/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package datagen.rtti.abstract_super

import kotlin.test.*

abstract class Super

class Foo : Super()

@Test fun runTest() {
    // This test now checks that the source can be successfully compiled and linked;
    // TODO: check the contents of TypeInfo?
}
