//FILE: GetterSetterExample.java

import lombok.*;
import org.jetbrains.annotations.*;

@Getter @Setter
public class GetterSetterExample {
    @NotNull
    private Integer age = 10;
    @Nullable
    private String name;
}


//FILE: test.kt

class Test {
    fun run() {
        val obj = GetterSetterExample()
        val age: Int = obj.getAge()
        val name: String? = obj.getName()
    }
}
