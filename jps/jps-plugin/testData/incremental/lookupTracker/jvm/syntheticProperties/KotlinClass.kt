package foo

/*p:<root>(JavaClass)*/import JavaClass

/*p:foo*/class KotlinClass : /*p:<root> p:foo*/JavaClass() {
    override fun getFoo() = 2
    fun setFoo(i: /*p:<root> p:foo*/Int) {}
}
