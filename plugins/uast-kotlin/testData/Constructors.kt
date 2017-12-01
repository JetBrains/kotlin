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
        // This body will not be picked by UAST because no lightMethod contains it.
        // Of course it is not a desired behaviour, so fix it if you know how
        println()
    }

    constructor(i: Int) {
        a = i.toString()
    }

    constructor(s: String) {
        a = s
    }

}