package test

class ClassWithPrivateSecondaryConstructorsAdded() {
    private constructor(arg: Int) : this() {}
    private constructor(arg: String) : this() {}
}

class ClassWithPrivateSecondaryConstructorsAdded2() {
    private constructor(arg: Int) : this() {}
    private constructor(arg: String) : this() {}
    constructor(arg: Float) : this() {}
}

class ClassWithPrivateSecondaryConstructorsRemoved() {
}
