// FILE: ClassLevelGetterTest.java

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class ClassLevelGetterTest {
    private int age = 10;

    @Getter(AccessLevel.PROTECTED) private String name;

    private boolean primitiveBoolean;

    private Boolean boxedBoolean;

    void test() {
        getAge();
        isPrimitiveBoolean();
    }

}


// FILE: test.kt

fun test() {
    val obj = ClassLevelGetterTest()
    val getter = obj.getAge()
    val property = obj.age

    obj.isPrimitiveBoolean()

    obj.boxedBoolean
    obj.getBoxedBoolean()

    //shouldn't be accesible from here
    obj.<!INVISIBLE_REFERENCE!>getName<!>()

    OverridenGetterTest().usage()
}

class OverridenGetterTest : ClassLevelGetterTest() {
    fun usage() {
        getName()
    }
}
