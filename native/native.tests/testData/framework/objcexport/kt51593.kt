/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:MyCustomFileAspect(type = "kt")

@Target(AnnotationTarget.FILE)
annotation class MyCustomFileAspect(val type: String = "kt")

annotation class NoDoc
@MustBeDocumented
annotation class Foo
@MustBeDocumented
annotation class BugReport(
        val assignedTo: String = "[none]",
        val status: String = "open"
)

@NoDoc
@Foo
@BugReport(assignedTo = "me", status = "open")
interface MyInterface {
}

@NoDoc
@Foo
@BugReport(assignedTo = "me", status = "open")
class Bar {
    @NoDoc
    @Foo
    @Deprecated("warning", level = DeprecationLevel.WARNING)
    /**
     * My method
     *   @param nodocParam is one arg
     *  @param fooParam is second arg
     * @return their sum
     */
    protected suspend fun baz (@NoDoc nodocParam:Int, @Foo @BugReport(assignedTo = "me", status = "fixed") fooParam:Int): Int { return nodocParam + fooParam }

    /** My property
     ***
     *
     */
    @Foo
    @BugReport(assignedTo = "me", status = "open")
    val greeting: String
        get() {
            return "Hello World!"
        }

    // Not a kDoc-formatted comment
    protected val farewell: String
        get() { return "Bye bye!" }
}
