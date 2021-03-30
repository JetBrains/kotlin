package foo

import test.A.Companion.CONSTANT_VALUE

class B {
    companion object {
        fun main() {
            println("Import companion constant: ${CONSTANT_VALUE}")
        }

    }

}