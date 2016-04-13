// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintMissingSuperCallInspection

package android.support.annotation

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class CallSuper

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class CallSuperTest {
    private class Child : Parent() {
        override fun <error descr="Overriding method should call `super.test1`">test1</error>() {
            // ERROR
        }

        override fun <error descr="Overriding method should call `super.test2`">test2</error>() {
            // ERROR
        }

        override fun <error descr="Overriding method should call `super.test3`">test3</error>() {
            // ERROR
        }

        override fun <error descr="Overriding method should call `super.test4`">test4</error>(arg: Int) {
            // ERROR
        }

        override fun test4(arg: String) {
            // OK
        }


        override fun <error descr="Overriding method should call `super.test5`">test5</error>(arg1: Int, arg2: Boolean, arg3: Map<List<String>, *>, // ERROR
                           arg4: Array<IntArray>, vararg arg5: Int) {
        }

        override fun <error descr="Overriding method should call `super.test5`">test5</error>() {
            // ERROR
            super.test6() // (wrong super)
        }

        override fun test6() {
            // OK
            val x = 5
            super.test6()
            System.out.println(x)
        }
    }

    private open class Parent : ParentParent() {
        @CallSuper
        protected open fun test1() {
        }

        override fun test3() {
            super.test3()
        }

        @CallSuper
        protected open fun test4(arg: Int) {
        }

        protected open fun test4(arg: String) {
        }

        @CallSuper
        protected open fun test5() {
        }

        @CallSuper
        protected open fun test5(arg1: Int, arg2: Boolean, arg3: Map<List<String>, *>,
                                 arg4: Array<IntArray>, vararg arg5: Int) {
        }
    }

    private open class ParentParent : ParentParentParent() {
        @CallSuper
        protected open fun test2() {
        }

        @CallSuper
        protected open fun test3() {
        }

        @CallSuper
        protected open fun test6() {
        }

        @CallSuper
        protected fun test7() {
        }


    }

    private open class ParentParentParent
}
