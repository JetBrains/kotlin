package test

class ClassWithPrivatePrimaryConstructorAdded private constructor() {
    private constructor(arg: Int) : this() {}
}

class ClassWithPrivatePrimaryConstructorRemoved {
    private constructor(arg: Int) {}
}

class ClassWithPrivatePrimaryConstructorChanged private constructor(arg: String) {
}
