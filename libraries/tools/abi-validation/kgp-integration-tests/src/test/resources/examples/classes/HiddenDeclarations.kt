/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package examples.classes

import annotations.*

@HiddenFunction
public fun hidden() = Unit

@HiddenProperty
public val v: Int = 42

@HiddenClass
public class HC

public class VC @HiddenCtor constructor() {
    @HiddenProperty
    public val v: Int = 42

    public var prop: Int = 0
        @HiddenGetter
        get() = field
        @HiddenSetter
        set(value) {
            field = value
        }

    @HiddenProperty
    public var fullyHiddenProp: Int = 0

    @HiddenFunction
    public fun m() = Unit
}

@HiddenClass
public class HiddenOuterClass {
    public class HiddenInnerClass {

    }
}
