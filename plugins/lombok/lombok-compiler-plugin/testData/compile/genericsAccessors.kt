//FILE: GenericsTest.java

import lombok.*;
import java.util.*;

public class GenericsTest<A, B> {
    @Getter private int age = 10;

    @Getter @Setter private A fieldA;

    @Getter private B fieldB;

    @Setter private Map<A, B> fieldC;

    static void test() {
        val obj = new GenericsTest<String, Boolean>();
        int age = obj.getAge();
        String a = obj.getFieldA();
        obj.setFieldA("fooo");
        Boolean b = obj.getFieldB();
        obj.setFieldC(new HashMap<String, Boolean>());
    }

}


//FILE: test.kt

class Test {
    fun run() {
        val obj = GenericsTest<String, Boolean>()
        val age: Int = obj.getAge();
        obj.setFieldA("fooo");
        val a: String = obj.getFieldA();
        val b: Boolean? = obj.getFieldB();
        obj.setFieldC(java.util.HashMap<String, Boolean>());
    }
}
