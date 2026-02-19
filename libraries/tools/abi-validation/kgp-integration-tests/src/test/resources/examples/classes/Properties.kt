/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package foo

@Target(AnnotationTarget.FIELD)
annotation class HiddenField

@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

public class ClassWithProperties {
    @HiddenField
    var bar1 = 42

    @HiddenProperty
    var bar2 = 42
}

