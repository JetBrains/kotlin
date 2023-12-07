/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kdocExport

/**
 * `Summary class` [KDocExport]. // EXPORT_KDOC
 *
 * @property xyzzy Doc for property xyzzy // EXPORT_KDOC
 * @property zzz See below. // EXPORT_KDOC
 */

// Expected: this comment shall not affect KDoc (i.e. kdoc above is still OK)
class KDocExport() {
    /**
     * @param xyzzy is documented. // EXPORT_KDOC
     *
     * This is multi-line KDoc. See a blank line above. // EXPORT_KDOC
     */
    val xyzzy: String = "String example"

    /** Non-primary ctor KDoc: // EXPORT_KDOC */
    constructor(name: String) : this() {
        println(name)
    }

    /** @property xyzzy KDoc for foo? // EXPORT_KDOC */
    val foo = "foo"
    /** @property foo KDoc for yxxyz? // EXPORT_KDOC */
    var yxxyz = 0;
}


/**
 * Useless function [whatever] // EXPORT_KDOC
 *
 * This kdoc has some additional formatting. // EXPORT_KDOC
 * @param a keep intact and return // EXPORT_KDOC
 * @return value of [a] // EXPORT_KDOC
 * Check for additional comment (note) below // EXPORT_KDOC
 */
fun whatever(a:String) = a

public abstract class SomeClassWithProperty
{
    /**
     * Kdoc for a property // EXPORT_KDOC
     */
    public abstract val heavyFormattedKDocFoo: SomeClassWithProperty
}