package javaApi

import kotlinApi.KotlinClass

public abstract class C(field: Int) : KotlinClass(field) {

    override fun foo(mutableCollection: MutableCollection<String>, nullableCollection: Collection<out Int>?): MutableList<Any> {
        return super.foo(mutableCollection, nullableCollection)
    }
}
