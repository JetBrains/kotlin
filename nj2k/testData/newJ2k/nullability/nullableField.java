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

    public void test1() {
        foo1(myProp);
    }

    public void test2() {
        foo2(myProp);
    }

    public void test3() {
        foo3(myProp);
    }

    public void test4() {
        myProp.charAt(myIntProp);
        System.out.println(myProp);
    }

    public void test5() {
        boolean b = "aaa".equals(myProp);
        String s = "aaa" + myProp;
    }

    public void test6() {
        myProp.compareToIgnoreCase(myProp);
    }

    public void test7() {
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