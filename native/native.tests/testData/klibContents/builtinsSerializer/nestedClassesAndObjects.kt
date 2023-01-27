// KT-56190 K2 does not emit const initializers
// MUTED_WHEN: K2
package test

class ClassA {
    class classB {
        fun memberFromB(): Int = 100

        class BC {
            val memberFromBB: Int = 150
        }

        object BO {
            val memberFromBO: Int = 175
        }
    }

    inner class classC {
        val memberFromC: Int = 200
    }

    companion object {
        val stat: Int = 250

        class D {
            val memberFromD: Int = 275
        }
    }

    object ObjA {
        val memberFromObjA: Int = 300
    }
}
