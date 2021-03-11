package test

class ClassWithAddedCompanionObject {
    public fun unchangedFun() {}
}

class ClassWithRemovedCompanionObject {
    public fun unchangedFun() {}
    companion object {}
}

class ClassWithChangedCompanionObject {
    public fun unchangedFun() {}
    companion object FirstName {}
}

class ClassWithChangedVisibilityForCompanionObject {
    public fun unchangedFun() {}
    public companion object {}
}

