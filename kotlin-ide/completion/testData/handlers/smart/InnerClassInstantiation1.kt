class A {
    inner class XYZ

    fun foo() {
        val v: XYZ = <caret>
    }
}

// ELEMENT: XYZ
