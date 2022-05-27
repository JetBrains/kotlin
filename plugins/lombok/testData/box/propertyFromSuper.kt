// KT-47455

// FILE: ParentClass.java
public abstract class ParentClass {
    int id;
}

// FILE: ChildClass.java
import lombok.*;

@RequiredArgsConstructor
public class ChildClass extends ParentClass {}


// FILE: test.kt
fun box(): String {
    ChildClass::class
    return "OK"
}
