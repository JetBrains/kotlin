// DISABLE-ERRORS

@RequiresOptIn
annotation class FirstExperience

open class ParentTarget {
    @FirstExperience open fun targetFun() {}
}

class ChildTarget : ParentTarget() {
    <caret>
}