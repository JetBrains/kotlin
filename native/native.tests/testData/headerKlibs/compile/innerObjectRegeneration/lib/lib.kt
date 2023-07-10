package lib

var result = "fail"

inline fun foo(crossinline s: () -> String) {
    object {
        private inline fun test(crossinline z: () -> String) {
            object {
                fun run() {
                    result = z()
                }
            }.run()
        }

        fun foo() {
            test { s() } // regenerated object should be marked as public abi
        }
    }.foo()
}
