// FILE: SetterTest.java

import lombok.AccessLevel;
import lombok.Setter;
import lombok.Getter;
import lombok.experimental.Accessors;

@Setter
@Getter
public class SetterTest {
    @Accessors(fluent = true) private int fluent;

    @Accessors(chain = true) private String chained;

    @Accessors(chain = true, fluent = true) private String whyNotBoth;


    void test() {
        fluent(12);
        setChained("zz").getChained();
        whyNotBoth("zzz").whyNotBoth();
    }
}


// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val obj = SetterTest()
    obj.fluent(12)
    assertEquals(12, obj.fluent())
    obj.setChained("zz").getChained()
    assertEquals("zz", obj.getChained())
    obj.whyNotBoth("zzz").whyNotBoth()
    assertEquals("zzz", obj.whyNotBoth())
    return "OK"
}
