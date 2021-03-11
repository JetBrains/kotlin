package foo

class A {
    fun classMember() {

    }

    companion object {
        fun companionMember() {

        }

        fun Int.companionExtensionMember() {

        }
    }
}

fun A.classExtension() {

}

object O {
    fun objectMember1() {

    }

    fun objectMember2() {

    }

    fun Int.objectExtensionMember1() {

    }

    fun Int.objectExtensionMember2() {

    }
}

fun O.objectExtension() {

}

fun A.Companion.companionExtension() {

}

fun topLevel() {

}