// FILE: FluentTest.java

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class FluentTest {
    @Getter private int age = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    @Getter private boolean primitiveBoolean;

    @Getter private Boolean boxedBoolean;

    void test() {
        age();
        primitiveBoolean();
    }

}


// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val obj = FluentTest()
    assertEquals(10, obj.age())

    obj.primitiveBoolean()

    obj.boxedBoolean()

    OverridenGetterTest().usage()
    return "OK"
}

class OverridenGetterTest : FluentTest() {
    fun usage() {
        name()
    }
}
