package foo

/*p:<root>(JavaClass)*/import JavaClass

/*p:foo*/class KotlinClass : /*p:<root>*/JavaClass() {
    override fun getFoo() = /*p:kotlin(Int)*/2
    fun setFoo(i: /*c:JavaClass c:foo.KotlinClass p:foo p:kotlin*/Int) {}
}
