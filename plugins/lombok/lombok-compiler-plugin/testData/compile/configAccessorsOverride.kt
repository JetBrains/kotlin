//FILE: GetterTest.java

import lombok.*;
import lombok.experimental.*;

@Getter @Setter
public class GetterTest {
    private String name;
    private int age;
    @Accessors(chain = true)
    private boolean fluent;

    static void test() {
        val obj = new GetterTest();
        GetterTest ref = obj.fluent(true);
        obj.name();
        obj.age();
    }

}


//FILE: test.kt

class Test {
    fun run() {
        val obj = GetterTest()
        val ref: GetterTest = obj.fluent(true)
        obj.name()
        obj.age()
    }
}

//FILE: lombok.config
lombok.accessors.fluent=true
lombok.accessors.chain=false
