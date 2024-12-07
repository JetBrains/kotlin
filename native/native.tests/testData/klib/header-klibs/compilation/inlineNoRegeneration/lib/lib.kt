package lib

var result = "fail"

inline fun foo(crossinline s: () -> String) {
    object {
        private inline fun test(crossinline z: () -> String) {
            result = object { //should be marked as public abi as there is no regenerated abject on inline
                fun run(): String {
                    return "O"
                }
            }.run() + z()
        }

        fun foo() {
            test { s() }
        }
    }.foo()
}
