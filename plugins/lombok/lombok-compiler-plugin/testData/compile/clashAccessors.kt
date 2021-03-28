//FILE: SuperClass.java

import lombok.*;
import java.util.*;

public class SuperClass {

    public void setName(String name) {

    }

}


//FILE: ClashTest.java

import lombok.*;
import java.util.*;

@Getter
@Setter
public class ClashTest extends SuperClass {
    private int age = 10;

    private String name;

    private boolean human;

    private Integer toOverride;

    public int getAge() {
        return age;
    }

    public void setAge(String age) {

    }

    public boolean isHuman(String arg) {
        return human;
    }


    static void test() {
        val obj = new ClashTest();

        obj.getAge();
//        obj.setAge(41);

        obj.getName();
        obj.setName("Al");

        obj.isHuman();
        obj.setHuman(true);
        obj.isHuman("sdf");
    }

}

//FILE: ChildClass.java

import lombok.*;
import java.util.*;

public class ChildClass extends ClashTest{

    @Override
    public Integer getToOverride() {
        return super.getToOverride();
    }

}


//FILE: test.kt

class KotlinChildClass : ClashTest() {

    override fun getToOverride(): Int = super.getToOverride()

}

class Test {
    fun run() {
        val obj = ClashTest()

        obj.getAge()
        //todo thats shouldn't work
        obj.setAge(41)
        val age = obj.age
        obj.age = 12


        obj.getName()
        obj.setName("Al")
        val name = obj.name
        obj.name = "sdf"

        obj.isHuman()
        obj.setHuman(true)
        obj.isHuman("sdf")
        val isHuman = obj.isHuman
        obj.isHuman = false

        val childObj = KotlinChildClass()
        childObj.getToOverride()
        childObj.setToOverride(34)
        childObj.toOverride
        childObj.toOverride = 412
    }
}
