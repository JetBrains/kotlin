class Single {

    val x: Int

    constructor(x: Int) {
        this.x = x
    }
}

class NotSingle {

    val x: Int

    constructor(): this(42)

    constructor(x: Int) {
        this.x = x
    }
}