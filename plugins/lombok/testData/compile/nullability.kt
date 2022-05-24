// FILE: GetterSetterExample.java

import lombok.*;
import org.jetbrains.annotations.*;

@Getter @Setter
public class GetterSetterExample {
    @NotNull
    private Integer age = 10;
    @Nullable
    private String name;
}


// FILE: test.kt

fun box(): String {
    val obj = GetterSetterExample()
    val age: Int = obj.getAge()
    val name: String? = obj.getName()
    return "OK"
}
