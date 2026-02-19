/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")
package cases.default

class ClassConstructors
internal constructor(name: String, flags: Int = 0) {

    internal constructor(name: StringBuilder, flags: Int = 0) : this(name.toString(), flags)

}

