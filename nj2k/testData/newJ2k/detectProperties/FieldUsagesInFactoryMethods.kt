internal class C(val arg1: Int) {
    val arg2: Int
    var arg3: Int

    constructor(arg1: Int, arg2: Int, arg3: Int) : this(arg1) {
        this.arg2 = arg2
        this.arg3 = arg3
    }

    constructor(arg1: Int, arg2: Int) : this(arg1) {
        this.arg2 = arg2
        arg3 = 0
    }

    init {
        arg2 = 0
        arg3 = 0
    }
}
