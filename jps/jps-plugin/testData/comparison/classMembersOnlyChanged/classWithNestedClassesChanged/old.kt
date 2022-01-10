package test

class ClassWithNestedClasses {
    class NestedClassRemoved {}
    inner class InnerClass {}
    public fun unchangedFun() {}
}

class ClassWithChangedVisibilityForNestedClasses {
    class NestedClass {}
    inner class InnerClass {}
    public fun unchangedFun() {}
}

