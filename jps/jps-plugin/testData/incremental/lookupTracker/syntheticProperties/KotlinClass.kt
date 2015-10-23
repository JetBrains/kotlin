package foo

import /*p:<root>*/JavaClass

/*p:foo*/class KotlinClass : JavaClass() {
    override fun getFoo() = 2
    fun setFoo(i: /*c:foo.KotlinClass p:foo*/Int) {}
}
