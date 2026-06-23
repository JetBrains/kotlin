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

    @Getter
    private String имя;

    @Getter
    private String ы;

    @Getter
    private String ßßß;

    @Getter
    private String ß;
}

// FILE: UsageFromKotlin.kt

fun box(): String {
    val obj = GetterSetterExample()
    obj.name = "John"
    obj.setName("John")
    
    obj.age
    obj.getAge()

    // Support case transformation for non-Latin letters (e.g., Cyrillic).
    obj.getИмя()
    obj.getЫ()

    // Preserve special characters in the leading letter without case expansion (e.g., ß is not converted to SS)
    obj.getßßß()
    obj.getß()

    return "OK"
}
