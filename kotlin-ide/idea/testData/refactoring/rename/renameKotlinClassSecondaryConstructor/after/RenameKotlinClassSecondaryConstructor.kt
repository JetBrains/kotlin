open class TestX {
    constructor(x: Int) {}
}

class Y1 : TestX(1)
class Y2 : TestX {
    constructor(): super(1) {}
}

val x = TestX(1)
