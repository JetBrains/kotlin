// FIR_IDENTICAL
// FILE: Anno.java

import java.lang.*;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Anno {
    String value() default "sdf";
}

// FILE: Foo.java

import lombok.*;
import org.jetbrains.annotations.*;

public class Foo {
    private Integer age = 10;
    private String name;
}


// FILE: test.kt

<!WRONG_ANNOTATION_TARGET!>@Anno<!>
fun box(): String {
    val obj = Foo()
    return "OK"
}
