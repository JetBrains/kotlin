package test;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import java.util.Collections;
import java.util.List;

class Test {
    public static Java8Class field = new Java8Class();
    public static Java8Class staticFun() {
        return new Java8Class();
    }
    public int memberFun() {return 1;}

    public static String testOverloads() {
        return "1";
    }

    public static String testOverloads(int i) {
        return "2";
    }
}

class Java8Class {
    private Java8Class field = new Java8Class();

    public void testStaticFunction() {
        Function0 staticFunFromSameClass = Java8Class::staticFun;
        staticFunFromSameClass.invoke();

        Function0 staticFunFromAnotherClass = Test::staticFun;
        staticFunFromAnotherClass.invoke();
    }

    public void testMemberFunctionThroughClass() {
        Function1<Java8Class, Integer> memberFunFromClass = Java8Class::memberFun;
        memberFunFromClass.invoke(new Java8Class());
    }

    public void testMemberFunctionThroughObject() {
        Java8Class obj = new Java8Class();
        Function0 memberFunFromSameClass = obj::memberFun;
        memberFunFromSameClass.invoke();

        Test anotherObj = new Test();
        Function0 memFunFromAnotherClass = anotherObj::memberFun;
        memFunFromAnotherClass.invoke();

        Function0 memberFunThroughObj1 = field::memberFun;
        memberFunThroughObj1.invoke();
        Function0 memberFunThroughObj2 = Test.field::memberFun;
        memberFunThroughObj2.invoke();
        Function0 memberFunThroughObj3 = Test.staticFun()::memberFun;
        memberFunThroughObj3.invoke();
    }

    public void testConstructor() {
        Function0 constructorSameClass = Java8Class::new;
        constructorSameClass.invoke();

        Function0 qualifiedConstructorSameClass = test.Java8Class::new;
        qualifiedConstructorSameClass.invoke();

        Function0 constructorAnotherClass = Test::new;
        constructorAnotherClass.invoke();

        Function0 qualifiedConstructorAnotherClass = test.Test::new;
        qualifiedConstructorAnotherClass.invoke();
    }

    public void testLibraryFunctions() {
        Function1<String, Integer> memberFunFromClass = String::length;
        memberFunFromClass.invoke("str");
    }

    public void testOverloads() {
        Function0<String> constructorWithoutParams = Test::testOverloads;
        constructorWithoutParams.invoke();

        Function1<Integer, String> constructorWithParam = Test::testOverloads;
        constructorWithParam.invoke(2);
    }

    public void testGenericFunctions() {
        Function0<List<String>> emptyList = Collections::emptyList;
        emptyList.invoke();
    }

    public static int staticFun() { return 1; }

    public int memberFun() { return 1; }

    public Java8Class() {}
}