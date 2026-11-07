// MODULE: main
// FILE: example/JavaTypes.java
package example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
@interface TypeLabel {}

public class JavaTypes {
    public @TypeLabel String annotated;
}
