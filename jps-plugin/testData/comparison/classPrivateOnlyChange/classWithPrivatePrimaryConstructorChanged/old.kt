package test

class ClassWithPrivatePrimaryConstructorAdded {
    private constructor(arg: Int) {}
}

class ClassWithPrivatePrimaryConstructorRemoved private constructor() {
    private constructor(arg: Int) : this() {}
}

class ClassWithPrivatePrimaryConstructorChanged private constructor() {
}
