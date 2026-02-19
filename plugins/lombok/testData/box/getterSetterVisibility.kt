// ISSUE: KT-83063

// FILE: GetterSetterExample.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class GetterSetterExample {
    @Setter(AccessLevel.PROTECTED)
    @Getter
    private String name;

    @Setter
    @Getter(AccessLevel.PROTECTED)
    private int age;
}

// FILE: UsageFromKotlin.kt

fun box(): String {
    val obj = GetterSetterExample()
    obj.name = "John"
    obj.setName("John")
    
    obj.age
    obj.getAge()
    return "OK"
}
