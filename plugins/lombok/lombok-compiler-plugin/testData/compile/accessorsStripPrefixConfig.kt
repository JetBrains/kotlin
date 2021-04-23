//FILE: AccessorsTest.java

import lombok.*;
import lombok.experimental.*;

@Getter @Setter
public class AccessorsTest {
//    @Accessors
    private int age = 10;
    private int fTarget = 42;
    private String fieldValue;

    @Accessors(prefix = {})
    private boolean isHuman;
    private boolean fPrefixedBoolean;
    @Accessors(prefix = {})
    private Boolean isNonPrimitiveHuman;

    static void test() {
        val obj = new AccessorsTest();

//        obj.getAge();
//        obj.setAge(123);

        obj.getTarget();
        obj.setTarget(34);

        obj.getValue();
        obj.setValue("sdf");

        obj.isHuman();
        obj.setHuman(true);

        obj.isPrefixedBoolean();
        obj.setPrefixedBoolean(false);

        obj.getIsNonPrimitiveHuman();
        obj.setIsNonPrimitiveHuman(false);
    }

}


//FILE: test.kt

class Test {
    fun run() {
        val obj = AccessorsTest()

//        obj.getAge()
//        obj.setAge(123)

        obj.getTarget()
        obj.setTarget(34)

        obj.getValue()
        obj.setValue("sdf")

        obj.isHuman()
        obj.setHuman(true)

        obj.isPrefixedBoolean()
        obj.setPrefixedBoolean(false)

        obj.getIsNonPrimitiveHuman()
        obj.setIsNonPrimitiveHuman(false)

    }

}

//FILE: lombok.config
lombok.accessors.prefix += f
lombok.accessors.prefix+=field
