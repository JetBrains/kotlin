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

object Test {
    fun usage() {
        val obj = FluentTest()
        val getter = obj.age()

        obj.primitiveBoolean()

        obj.boxedBoolean()

        obj.overrideAnnotation
        obj.getOverrideAnnotation()
    }

    class OverridenGetterTest : FluentTest() {
        fun usage() {
            name()
        }
    }
}
