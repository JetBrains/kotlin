//FILE: SetterTest.java

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


//FILE: test.kt

class Test {
    fun run() {
        val obj = SetterTest()
        obj.setAge(42)
        assertEquals(obj.age, 42)
        obj.age = 43
        assertEquals(obj.age, 43)

        obj.setPrimitiveBoolean(true)

        
//        no setters generated for final variable
//        obj.setFinalName("error")
    }
}
