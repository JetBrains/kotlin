/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kt43599

// Note: this test relies on two-stage compilation.

class KT43599 {
    var memberProperty = "memberProperty"
        private set
}

var KT43599.extensionProperty
    get() = "extensionProperty"
    private set(value) { TODO() }

var topLevelProperty
    get() = "topLevelProperty"
    private set(value) { TODO() }

lateinit var topLevelLateinitProperty: String
    private set

fun setTopLevelLateinitProperty(value: String) {
    topLevelLateinitProperty = value
}
