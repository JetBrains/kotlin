package test

class ClassWithPrivateSecondaryConstructorsAdded {
}

class ClassWithPrivateSecondaryConstructorsAdded2() {
    constructor(arg: Float) : this() {}
}

class ClassWithPrivateSecondaryConstructorsRemoved() {
    private constructor(arg: Int): this() {}
    private constructor(arg: String): this() {}
}
