/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.special

@Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
public class HiddenClass
    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor() {

    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    val hiddenVal = 1

    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    var hiddenVar = 2

    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    fun hiddenFun() {}

    public var varWithHiddenAccessors: String = ""
        @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
        get
        @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
        set
}

@Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
fun hiddenTopLevelFun() {}
