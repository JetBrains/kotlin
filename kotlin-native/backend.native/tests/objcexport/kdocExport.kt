/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kdocExport

/**
 * Summary class [KDocExport]
 * @property xyzzy Doc for property xyzzy
 * @property zzz See below.
 */

// Expected: this comment shall not affect KDoc (i.e. kdoc above is still OK)
class KDocExport() {
    /**
     * @param xyzzy is documented.
     * This is multi-line KDoc
     */
    val xyzzy: String = "String example"

    /** Non-primary ctor KDoc: */
    constructor(name: String) : this() {
        println(name)
    }

    /** @property xyzzy KDoc for foo? */
    val foo = "foo"
    /** @property foo KDoc for yxxyz? */
    var yxxyz = 0;
}
