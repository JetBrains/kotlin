/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.consts

class WithInternalCompanion private constructor() {
    internal companion object {
        const val x: Int = 0
    }
}

class WithPrivateCompanion private constructor() {
    private companion object {
        const val y: Int = 0
    }
}

class WithPublishedCompanion private constructor() {
    @PublishedApi
    internal companion object {
        const val z: Int = 0
    }
}
