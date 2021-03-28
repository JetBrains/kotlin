//FILE: SetterTest.java

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


//FILE: test.kt

class Test {
    fun run() {
        val obj = SetterTest()
        obj.fluent(12)
        obj.setChained("zz").getChained()
        obj.whyNotBoth("zzz").whyNotBoth()
    }

}
