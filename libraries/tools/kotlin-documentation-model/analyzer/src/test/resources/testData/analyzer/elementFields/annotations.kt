// MODULE: main
// FILE: module-info.java
@Deprecated
module annotated.elements {
    exports sample;
}

// FILE: sample/package-info.java
/** An annotated package. */
@Deprecated
@PackageLabel("sample package")
package sample;

// FILE: sample/PackageLabel.java
package sample;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.PACKAGE)
public @interface PackageLabel {
    String value();
}

// FILE: sample/Declarations.kt
package sample

@MustBeDocumented
annotation class Label(val text: String, val number: Int = 42)

@Label("class")
class Annotated @Label("constructor") constructor() {
    @Label("function", 7)
    fun annotated() {}
    @Label("property")
    val property: String = ""
    @get:Label("getter")
    @set:Label("setter")
    var accessors: String = ""
    fun plain() {}
}
@Label("alias")
typealias AnnotatedAlias = String
fun plain() {}

// FILE: sample/AnnotatedJava.java
package sample;
@Deprecated
public class AnnotatedJava {
    @Deprecated
    public void annotated() {}
    public void plain() {}
}
