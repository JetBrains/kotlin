/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.marker

@Target(AnnotationTarget.FIELD)
annotation class HiddenField

@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

annotation class HiddenMethod

public class Foo {
    // HiddenField will be on the field
    @HiddenField
    var bar1 = 42

    // HiddenField will be on a synthetic `$annotations()` method
    @HiddenProperty
    var bar2 = 42

    @HiddenMethod
    fun hiddenMethod(bar: Int = 42) {
    }
}

