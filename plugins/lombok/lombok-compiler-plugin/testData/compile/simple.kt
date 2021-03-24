//FILE: GetterSetterExample.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class GetterSetterExample {
    @Getter @Setter private int age = 10;
    
    @Getter(AccessLevel.PROTECTED) private String name;
}


//FILE: test.kt

object Test {
    fun usage() {
        val obj = GetterSetterExample()
        val getter = obj.getAge()
        val property = obj.age
    }
}
