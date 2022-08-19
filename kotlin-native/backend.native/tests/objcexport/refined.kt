/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package refined

import kotlin.experimental.ExperimentalObjCRefinement

@ExperimentalObjCRefinement
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@HidesFromObjC
annotation class MyHiddenFromObjC

@ExperimentalObjCRefinement
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@RefinesInSwift
annotation class MyShouldRefineInSwift

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
fun foo(): Int = 1

@OptIn(ExperimentalObjCRefinement::class)
@ShouldRefineInSwift
fun fooRefined(): String = foo().toString()

@OptIn(ExperimentalObjCRefinement::class)
@MyHiddenFromObjC
fun myFoo(): Int = 2

@OptIn(ExperimentalObjCRefinement::class)
@MyShouldRefineInSwift
fun myFooRefined(): String = myFoo().toString()

@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
val bar: Int = 3

@OptIn(ExperimentalObjCRefinement::class)
@ShouldRefineInSwift
val barRefined: String get() = bar.toString()

@OptIn(ExperimentalObjCRefinement::class)
@MyHiddenFromObjC
val myBar: Int = 4

@OptIn(ExperimentalObjCRefinement::class)
@MyShouldRefineInSwift
val myBarRefined: String get() = myBar.toString()

open class RefinedClassA {
    @OptIn(ExperimentalObjCRefinement::class)
    @HiddenFromObjC
    open fun foo(): Int = 1
    @OptIn(ExperimentalObjCRefinement::class)
    @ShouldRefineInSwift
    open fun fooRefined(): String = foo().toString()
}

class RefinedClassB: RefinedClassA() {
    override fun foo(): Int = 2
    override fun fooRefined(): String {
        val foo = foo()
        return "$foo$foo"
    }
}
