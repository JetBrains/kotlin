package test

class ClassWithPrimaryConstructorChanged constructor(arg: String) {
    public fun unchangedFun() {}
}

class ClassWithPrimaryConstructorVisibilityChanged private constructor() {
    public fun unchangedFun() {}
}

class ClassWithSecondaryConstructorsAdded() {
    constructor(arg: Int): this() {}
    constructor(arg: String): this() {}
    public fun unchangedFun() {}
}

class ClassWithSecondaryConstructorsRemoved() {
    public fun unchangedFun() {}
}

class ClassWithSecondaryConstructorVisibilityChanged() {
    private constructor(arg: Int): this() {}
    public fun unchangedFun() {}
}

