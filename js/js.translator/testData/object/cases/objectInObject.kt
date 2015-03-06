package foo

object A {
    object query {
        val status = "complete"
    }
}

object B {
    private val ov = "d"
    object query {
        val status = "complete" + ov
    }
}

class C {
    default object {
        fun ov() = "d"
    }
    object query {
        val status = "complete" + ov()
    }
}

fun box() = A.query.status == "complete" && B.query.status == "completed"
// todo fix after KT-3868 will be fixed
// && C.query.status == "completed"

