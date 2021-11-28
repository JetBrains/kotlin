import kt43517.*

fun produceEnum(): E =
        createEnum()

fun compareEnums(e1: E, e2: E): Boolean =
        e1 == e2

fun getFirstField(s: S): Int =
        s.i

fun getGlobalS(): S =
        globalS