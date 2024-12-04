package foo

import test.A

class C {
    companion object {
        fun main() {
            println("Companion constant: ${A.CONSTANT_VALUE}")
        }
    }

}