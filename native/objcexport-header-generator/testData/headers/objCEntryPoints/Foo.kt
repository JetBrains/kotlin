package pkg

class NoEntryPoints {
    val nonEntryPoint: Int = 0
    fun nonEntryPoint(): Int = 0
}

class ExplicitProperty {
    var entryPoint: Int = 0
    var nonEntryPoint: Int = 0
    fun entryPoint() {}
    fun entryPoint(i: Int) {}
}

class ExplicitFunction {
    fun entryPoint(): Int = 0
    fun entryPoint(i: Int): Int = 0
    fun nonEntryPoint(): Int = 0
    val entryPoint: Int = 0
}

class ExplicitCallable {
    var entryPoint: Int = 0
    fun entryPoint(): Int = 0
    fun entryPoint(i: Int): Int = 0
    var nonEntryPoint: Int = 0
    fun nonEntryPoint(): Int = 0
}

class Constructor {
    constructor()
    constructor(i: Int): this()
    var nonEntryPoint: Int = 0
    fun nonEntryPoint(): Int = 0
}

class WildcardProperty {
    var entryPoint1: Int = 0
    var entryPoint2: Int = 0
    fun nonEntryPoint(): Int = 0
}

class WildcardFunction {
    constructor()
    constructor(i: Int)
    fun entryPoint1(): Int = 0
    fun entryPoint1(i: Int): Int = 0
    fun entryPoint2(): Int = 0
    var nonEntryPoint: Int = 0
}

class WildcardCallable {
    constructor()
    constructor(i: Int)
    fun entryPoint1(): Int = 0
    fun entryPoint1(i: Int): Int = 0
    fun entryPoint2(): Int = 0
    var entryPoint1: Int = 0
    var entryPoint2: Int = 0
}
