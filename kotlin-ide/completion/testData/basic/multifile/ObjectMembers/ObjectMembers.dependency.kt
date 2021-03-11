package test

object KotlinObject {
    fun funFromObject() { }
    private fun funPrivate(){}
}

class KotlinClass {
    companion object SomeName {
        fun funFromCompanionObject() { }
        private fun funPrivate(){}
    }
}

class AnotherKotlinClass {
    private companion object {
        fun funFromPrivateCompanionObject() { }
    }

    object Nested {
        fun fromNested(){}
    }
}

interface I {
    fun fromInterface()
}

object DefaultI : I {
    override fun fromInterface() {
    }
}


fun foo() {
    val o = object : Runnable {
        override fun run() {
            fromAnonymous()
        }

        fun fromAnonymous(){}
    }
}
