// SCOPE_DUMP: ToStringOnJavaClass:toString
// DUMP_IR
// DUMP_EXTERNAL_CLASS: ToStringOnJavaClass

// FILE: ToStringOnJavaClass.java

import lombok.ToString;

@ToString
class ToStringOnJavaClass {
    public String name;
    public int age;

    public ToStringOnJavaClass(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: test.kt

fun box(): String {
    assertEquals("ToStringOnJavaClass(name=Alice, age=30)", ToStringOnJavaClass("Alice", 30).toString())
    return "OK"
}
