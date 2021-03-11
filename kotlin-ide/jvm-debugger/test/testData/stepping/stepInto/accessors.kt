package accessors

fun main(args: Array<String>) {
    A().test()
}

class A {
    fun test() {
        //Breakpoint!
        foo()
        prop
        prop = 2
    }

    companion object {
        private fun foo() {
            val a = 1
        }

        private var prop: Int = 2
            get() {
                return 1
            }
            set(i: Int) {
                field = i
            }
    }
}

// STEP_INTO: 17
// SKIP_SYNTHETIC_METHODS: false