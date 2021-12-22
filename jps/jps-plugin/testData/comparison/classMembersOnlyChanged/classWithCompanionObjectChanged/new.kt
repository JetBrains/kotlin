package test

class ClassWithAddedCompanionObject {
    public fun unchangedFun() {}
    companion object {}
}

class ClassWithRemovedCompanionObject {
    public fun unchangedFun() {}
}

class ClassWithChangedCompanionObject {
    public fun unchangedFun() {}
    companion object SecondName {}
}

class ClassWithChangedVisibilityForCompanionObject {
    public fun unchangedFun() {}
    private companion object {}
}
