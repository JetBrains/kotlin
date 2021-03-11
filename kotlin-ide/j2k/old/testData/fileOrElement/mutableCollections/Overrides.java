package javaApi;

import kotlinApi.KotlinClass;

import java.util.Collection;
import java.util.List;

public abstract class C extends KotlinClass {
    public C(int field) {
        super(field);
    }

    public List<Object> foo(Collection<String> mutableCollection, Collection<Integer> nullableCollection) {
        return super.foo(mutableCollection, nullableCollection);
    }
}
