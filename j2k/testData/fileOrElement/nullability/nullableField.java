package test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Test {
    private String myProp;
    private Integer myIntProp;

    public void onCreate() {
        myProp = "";
        myIntProp = 1;
    }

    public void test() {
        foo1(myProp);
        foo2(myProp);
        foo3(myProp);

        myProp.charAt(myIntProp);
        System.out.println(myProp);

        boolean b = "aaa".equals(myProp);
        String s = "aaa" + myProp;

        myProp.compareToIgnoreCase(myProp);

        List<Integer> list = new ArrayList<Integer>();
        list.remove(myIntProp);
    }

    public void foo1(@NotNull String s) {

    }

    public void foo2(String s) {

    }

    public void foo3(@Nullable String s) {

    }
}