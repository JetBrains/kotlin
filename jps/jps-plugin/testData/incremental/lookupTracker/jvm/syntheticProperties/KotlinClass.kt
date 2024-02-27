package foo

/*p:<root>(JavaClass)*/import JavaClass

/*p:foo*/class KotlinClass : /*p:foo*/JavaClass() {
    override fun getFoo() = 2
    fun setFoo(i: /*p:foo*/Int) {}
}
