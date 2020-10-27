/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


inline val Int.prop get() = SomeDataClass(second = this)

data class SomeDataClass(val first: Int = 17, val second: Int = 19, val third: Int = 23)

