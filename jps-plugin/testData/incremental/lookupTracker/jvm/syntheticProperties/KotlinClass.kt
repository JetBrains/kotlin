package foo

/*p:<root>(JavaClass)*/import JavaClass

/*p:foo*/class KotlinClass : /*p:<root>*/JavaClass() {
    override fun getFoo() = /*p:kotlin(Int)*/2
    fun setFoo(i: /*c:foo.KotlinClass c:JavaClass p:foo p:kotlin*/Int) {}
}
