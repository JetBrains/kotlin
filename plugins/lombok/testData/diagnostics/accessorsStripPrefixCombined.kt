// KT-46529

// FILE: PrefixJava.java

import lombok.*;
import lombok.experimental.*;

@Getter @Setter @Accessors(chain = false, fluent = true, prefix = {"pxo"})
public class PrefixJava {
    private String pxaPropA = "A";
    @Accessors(chain = true) private String pxoPropC = "C";
    @Accessors private String pxaPropD = "D";
}


// FILE: test.kt

fun test() {
    //not generated because doesn't have prefix from class level @Accessors
    val propA = PrefixJava().<!UNRESOLVED_REFERENCE!>propA<!>
    //not generated because doesn't have prefix from config
    val propC = PrefixJava().<!FUNCTION_CALL_EXPECTED!>propC<!>
    val propD = PrefixJava().<!UNRESOLVED_REFERENCE!>propD<!>
}

// FILE: lombok.config
lombok.accessors.prefix += pxa
