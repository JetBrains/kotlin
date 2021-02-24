/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.*

class ControlFlow {

    @Sample
    fun repeat() {
        // greets three times
        repeat(3) {
            println("Hello")
        }
        
        // greets with an index
        repeat(3) { index ->
            println("Hello with index $index")
        }

        repeat(0) {
            error("We should not get here!")
        }
    }
}
