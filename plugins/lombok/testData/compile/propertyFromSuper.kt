//KT-47455

//FILE: ParentClass.java
public abstract class ParentClass {
    int id;
}

//FILE: ChildClass.java
import lombok.*;

@RequiredArgsConstructor
public class ChildClass extends ParentClass {}


//FILE: test.kt
class Test {
    fun run() {
        ChildClass::class
    }
}