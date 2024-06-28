class Constructor private constructor() {}
class ConstructorParam0() {}
class ConstructorParam1(val a: Int) {}
class ConstructorParam2(val a: Int, val b: Int) {}

class ConstructorDouble(a: Int) {
    constructor(a: Int): this(a)
    constructor(a: Int, b: Int): this(b)
    constructor(a: Int, b: Int, c: Int): this(c)
}

class ConstructorFunction(foo: () -> Unit)