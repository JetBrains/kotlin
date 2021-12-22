package test

class ClassWithPrimaryConstructorChanged constructor() {
    public fun unchangedFun() {}
}

class ClassWithPrimaryConstructorVisibilityChanged constructor() {
    public fun unchangedFun() {}
}

class ClassWithSecondaryConstructorsAdded {
    public fun unchangedFun() {}
}

class ClassWithSecondaryConstructorsRemoved() {
    public constructor(arg: Int): this() {}
    constructor(arg: String): this() {}
    public fun unchangedFun() {}
}

class ClassWithSecondaryConstructorVisibilityChanged() {
    protected constructor(arg: Int): this() {}
    public fun unchangedFun() {}
}
