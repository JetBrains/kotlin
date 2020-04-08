class WithDifferentTypeProperty {
    val x: Number

    val y: String

    constructor(x: Int, <caret>z: String) {
        this.x = x
        this.y = z
    }
}