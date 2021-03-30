//FILE: AccessorsTest.java

import lombok.*;
import lombok.experimental.*;

@Getter @Setter
public class AccessorsTest {
    private int age = 10;

    private boolean isHuman;
    private Boolean isNonPrimitiveHuman;

    static void test() {
        val obj = new AccessorsTest();

        obj.getAge();

        obj.isHuman();
        obj.setHuman(true);

        obj.getIsNonPrimitiveHuman();
        obj.setIsNonPrimitiveHuman(false);
    }

}


//FILE: test.kt

class Test {
    fun run() {
        val obj = AccessorsTest()

        obj.getAge()
        val age = obj.age

        obj.isHuman()
        obj.setHuman(true)

    }

}
