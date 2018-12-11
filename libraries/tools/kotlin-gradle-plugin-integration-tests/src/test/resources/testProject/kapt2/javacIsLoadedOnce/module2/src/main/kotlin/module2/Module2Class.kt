/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package module2

import module1.Module1Class

@example.ExampleAnnotation
class Module2Class {
    @example.ExampleAnnotation
    fun testFunction(): Module2ClassGenerated = Module2ClassGenerated()

    fun useModule1Class(m: Module1Class) {
        m.testFunction()
    }
}
