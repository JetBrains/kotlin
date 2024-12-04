package foo

/*p:<root>(JavaClass)*/import JavaClass

/*p:foo*/class KotlinClass : /*p:<root>*/JavaClass() {
    override fun getFoo() = 2
    fun setFoo(i: /*p:JavaClass p:foo p:foo.KotlinClass*/Int) {}
}
