// ERROR: Cannot access 'staticField1': it is 'private' in 'Default'
// ERROR: Cannot access 'staticField2': it is 'private' in 'Default'
fun C(p: Int): C {
    val __ = C()
    System.out.println(C.staticField1 + C.staticField2)
    return __
}

class C {
    default object {
        private val staticField1 = 0
        private val staticField2 = 0
    }
}
