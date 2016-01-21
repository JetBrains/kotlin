package foo

import /*p:<root>*/JavaClass

/*p:foo*/class KotlinClass : JavaClass() {
    override fun getFoo() = /*p:kotlin(Int)*/2
    fun setFoo(i: /*c:foo.KotlinClass c:JavaClass p:foo*/Int) {}
}
