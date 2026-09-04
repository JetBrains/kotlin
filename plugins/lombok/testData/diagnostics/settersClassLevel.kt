// FILE: SetterTest.java

import lombok.AccessLevel;
import lombok.Setter;
import lombok.Getter;

@Getter @Setter
public class SetterTest {
    private int age = 10;

    private final String finalName = "zzz";

    private boolean primitiveBoolean;

    void test() {
        setAge(12);
        setPrimitiveBoolean(true);
        //no setters generated for final variable
//        setFinalName("adsf");
    }
}


// FILE: test.kt

fun test() {
    val obj = SetterTest()
    obj.setAge(42)
    val age = obj.age
    obj.age = 43
    val updatedAge = obj.age

    obj.setPrimitiveBoolean(true)

//    no setters generated for final variable
    obj.<!UNRESOLVED_REFERENCE!>setFinalName<!>("error")
}

