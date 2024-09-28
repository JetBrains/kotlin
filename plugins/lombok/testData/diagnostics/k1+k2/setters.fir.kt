// FILE: SetterTest.java

import lombok.AccessLevel;
import lombok.Setter;
import lombok.Getter;

public class SetterTest {
    @Getter @Setter private int age = 10;

    @Setter(AccessLevel.PROTECTED) private String name;

    @Setter private boolean primitiveBoolean;

    void test() {
        setAge(12);
        setPrimitiveBoolean(true);
    }
}


// FILE: test.kt

fun test() {
    val obj = SetterTest()
    obj.setAge(42)
    obj.age = 42

    //synthetic property generated only when there is a getter
    obj.<!INVISIBLE_REFERENCE!>primitiveBoolean<!> = false
    obj.setPrimitiveBoolean(true)

    //shouldn't be accesible from here
    obj.<!INVISIBLE_REFERENCE!>setName<!>("abc")

    OverridenGetterTest().usage()
}

class OverridenGetterTest : SetterTest() {
    fun usage() {
        setName("abc")
    }
}
