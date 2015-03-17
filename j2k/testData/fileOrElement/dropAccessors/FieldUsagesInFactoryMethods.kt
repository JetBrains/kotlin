class C(val arg1: Int) {
    var arg2: Int = 0
    var arg3: Int = 0

    constructor(arg1: Int, arg2: Int, arg3: Int) : this(arg1) {
        this.arg2 = arg2
        this.arg3 = arg3
    }

    constructor(arg1: Int, arg2: Int) : this(arg1) {
        this.arg2 = arg2
        arg3 = 0
    }

    {
        arg2 = 0
        arg3 = 0
    }
}
