//KT-46529

//FILE: PrefixJava.java

import lombok.*;
import lombok.experimental.*;

@Getter @Setter @Accessors(chain = false, fluent = true, prefix = {"pxo"})
public class PrefixJava {
    private String pxaPropA = "A";
    @Accessors(chain = true) private String pxoPropC = "C";
    @Accessors private String pxaPropD = "D";
}


//FILE: test.kt

class Test {
    fun run() {
        //not generated because doesn't have prefix from class level @Accessors
        //assertEquals(PrefixJava().propA, "A")
        //not generated because doesn't have prefix from config
        //assertEquals(PrefixJava().propC, "C")
        assertEquals(PrefixJava().propD, "D")
    }

}

//FILE: lombok.config
lombok.accessors.prefix += pxa
