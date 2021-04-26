//FILE: FluentTest.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class FluentTest {
    @Getter private int age = 10;

    @Getter @Accessors private int overrideAnnotation = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    @Getter private boolean primitiveBoolean;

    @Getter private Boolean boxedBoolean;

    void test() {
        age();
        primitiveBoolean();
        getOverrideAnnotation();
    }

}


//FILE: test.kt

class Test {
    fun run() {
        val obj = FluentTest()
        assertEquals(obj.age(), 10)

        obj.primitiveBoolean()

        obj.boxedBoolean()

        obj.overrideAnnotation
        obj.getOverrideAnnotation()

        OverridenGetterTest().usage()
    }

    class OverridenGetterTest : FluentTest() {
        fun usage() {
            name()
        }
    }
}
