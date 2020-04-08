class Cls {
    object Obj {
        val inObject = 1
    }
}

typealias TA = Cls.Obj

val usage = TA.<caret>

// EXIST: inObject