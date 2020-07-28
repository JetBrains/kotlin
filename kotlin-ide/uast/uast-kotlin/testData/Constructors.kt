class A(val str: String) {

    constructor(i: Int) : this(i.toString())

}

class AWithInit(val str: String) {

    init {
        println()
    }

    constructor(i: Int) : this(i.toString())

}

class AWith2Init(val str: String) {

    init {
        println(1)
    }

    init {
        println(2)
    }

    constructor(i: Int) : this(i.toString())

}

class AOnlyInit {

    init {
        println(1)
    }

    init {
        println(2)
    }
}

class AWithSecondary {

    lateinit var a: String

    constructor(i: Int) {
        a = i.toString()
    }

    constructor(s: String) {
        a = s
    }

}

class AWithSecondaryInit {

    lateinit var a: String

    init {
        println()
    }

    constructor(i: Int) {
        a = i.toString()
    }

    constructor(s: String) {
        a = s
        var local: String = s
        local.toString()
    }

}

class AWithFieldInit(i: Int) {
    val a: String
    init {
        a = i.toString()
    }
}
