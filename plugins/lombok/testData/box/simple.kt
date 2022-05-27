// FILE: GetterSetterExample.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class GetterSetterExample {
    @Getter @Setter private int age = 10;
    
    @Getter(AccessLevel.PROTECTED) private String name;
}


// FILE: test.kt

fun box(): String {
    val obj = GetterSetterExample()
    val getter = obj.getAge()
    val property = obj.age
    return "OK"
}
