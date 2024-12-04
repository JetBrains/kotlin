package test

class ClassWithNestedClasses {
    class NestedClassAdded {}
    inner class InnerClass {}
    inner class InnerClassAdded {}
    public fun unchangedFun() {}
}

class ClassWithChangedVisibilityForNestedClasses {
    private class NestedClass {}
    protected inner class InnerClass {}
    public fun unchangedFun() {}
}

