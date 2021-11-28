//FILE: GetterSetterExample.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class GetterSetterExample {
    @Getter @Setter private int age = 10;
    
    @Getter(AccessLevel.PROTECTED) private String name;
}


//FILE: test.kt

class Test {
    fun run() {
        val obj = GetterSetterExample()
        val getter = obj.getAge()
        val property = obj.age
    }
}
