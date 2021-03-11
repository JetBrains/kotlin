var prop: Int
    get() {
        print("get")
        val v = 1
        <caret>v
        return 1
    }
    set {
        print("set")
        val v = 2
        print("yes")
    }