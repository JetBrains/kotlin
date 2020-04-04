enum class JavaEnum {
    A("a"), B;

    constructor(x: String) {
        this.x = x
    }

    constructor() {
        x = "default"
    }

    protected var x: String
}