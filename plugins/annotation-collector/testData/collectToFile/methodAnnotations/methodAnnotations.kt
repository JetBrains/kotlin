package org.test

annotation class Ann

open class Parent {
    @Ann
    open fun overridenWithoutAnnotation() {}

    open fun overridenWithAnnotation() {}

    @Ann
    fun notOverriden() {}

    open fun notAnnotated() {}
}

class Child : Parent() {
    override fun overridenWithoutAnnotation() {}

    @Ann
    override fun overridenWithAnnotation() {}

    @Ann
    fun childMethod() {}
}