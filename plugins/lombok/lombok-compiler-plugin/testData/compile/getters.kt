//FILE: GetterTest.java

import lombok.AccessLevel;
import lombok.Getter;

public class GetterTest {
    @Getter private int age = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    @Getter private boolean primitiveBoolean;

    @Getter private Boolean boxedBoolean;

    @Getter(AccessLevel.NONE) private Boolean invisible;

    void test() {
        getAge();
        isPrimitiveBoolean();
//        getInvisible();
    }

}


//FILE: test.kt

class Test {
    fun run() {
        val obj = GetterTest()
        val getter = obj.getAge()
        val property = obj.age

        //todo kotlin doesn't see isBoolean methods as property
//        obj.primitiveBoolean
        obj.isPrimitiveBoolean()

        obj.boxedBoolean
        obj.getBoxedBoolean()

        //shouldn't be accesible from here
//        obj.getName()

//        obj.getInvisible()

        OverridenGetterTest().usage()
    }

    class OverridenGetterTest : GetterTest() {
        fun usage() {
            getName()
        }
    }
}
