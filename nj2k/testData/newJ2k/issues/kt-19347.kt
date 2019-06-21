package test

class TestMutltipleCtors {
    private var x: Int
    private var y = 0

    constructor(x: Int) {
        this.x = x
    }

    constructor(x: Int, y: Int) {
        this.x = x
        this.y = y
    }
}
