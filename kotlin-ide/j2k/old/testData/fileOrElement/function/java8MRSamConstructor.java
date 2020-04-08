package test;

import javaApi.*;

import java.lang.Integer;
import java.util.Collections;
import java.util.List;

class Test {
    public static Java8Class field = new Java8Class();
    public static Java8Class staticFun() {
        return new Java8Class();
    }
    public int memberFun() {
        return 1;
    }

    public static String testOverloads() {
        return "1";
    }

    public static String testOverloads(int i) {
        return "2";
    }

    public Test(int i) {
        super();
    }

    public Test() {
    }
}

class Test2 {}

class Java8Class {
    private Java8Class field = new Java8Class();
    private MethodReferenceHelperClass h = new MethodReferenceHelperClass();

    public void testStaticFunction() {
        JFunction0 staticFunFromSameClass = Java8Class::staticFun;
        staticFunFromSameClass.foo();
        MethodReferenceHelperClass.staticFun0(Java8Class::staticFun);
        h.memberFun0(Java8Class::staticFun);

        JFunction0 staticFunFromAnotherClass = Test::staticFun;
        staticFunFromAnotherClass.foo();
        MethodReferenceHelperClass.staticFun0(Test::staticFun);
        h.memberFun0(Test::staticFun);
    }

    public void testMemberFunctionThroughClass() {
        JFunction2<Java8Class, Integer> memberFunFromClass = Java8Class::memberFun;
        memberFunFromClass.foo(new Java8Class());
        MethodReferenceHelperClass.staticFun2(Java8Class::memberFun);
        h.memberFun2(Java8Class::memberFun);
    }

    public void testMemberFunctionThroughObject() {
        Java8Class obj = new Java8Class();
        JFunction0 memberFunFromSameClass = obj::memberFun;
        memberFunFromSameClass.foo();
        MethodReferenceHelperClass.staticFun0(obj::memberFun);
        h.memberFun0(obj::memberFun);

        Test anotherObj = new Test();
        JFunction0 memFunFromAnotherClass = anotherObj::memberFun;
        memFunFromAnotherClass.foo();
        MethodReferenceHelperClass.staticFun0(anotherObj::memberFun);
        h.memberFun0(anotherObj::memberFun);

        JFunction0 memberFunThroughObj1 = field::memberFun;
        memberFunThroughObj1.foo();
        MethodReferenceHelperClass.staticFun0(field::memberFun);
        h.memberFun0(field::memberFun);

        JFunction0 memberFunThroughObj2 = Test.field::memberFun;
        memberFunThroughObj2.foo();
        MethodReferenceHelperClass.staticFun0(Test.field::memberFun);
        h.memberFun0(Test.field::memberFun);

        JFunction0 memberFunThroughObj3 = Test.staticFun()::memberFun;
        memberFunThroughObj3.foo();
        MethodReferenceHelperClass.staticFun0(Test.staticFun()::memberFun);
        h.memberFun0(Test.staticFun()::memberFun);
    }

    public void testConstructor() {
        JFunction0 constructorSameClass = Java8Class::new;
        constructorSameClass.foo();
        MethodReferenceHelperClass.staticFun0(Java8Class::new);
        h.memberFun0(Java8Class::new);

        JFunction0 qualifiedConstructorSameClass = test.Java8Class::new;
        qualifiedConstructorSameClass.foo();
        MethodReferenceHelperClass.staticFun0(test.Java8Class::new);
        h.memberFun0(test.Java8Class::new);

        JFunction0 constructorAnotherClass = Test::new;
        constructorAnotherClass.foo();
        MethodReferenceHelperClass.staticFun0(Test::new);
        h.memberFun0(Test::new);

        JFunction2<Integer, Test> constructorAnotherClassWithParam = Test::new;
        constructorAnotherClassWithParam.foo(1);
        MethodReferenceHelperClass.<Integer, Test>staticFun2(Test::new);
        h.<Integer, Test>memberFun2(Test::new);

        JFunction0 qualifiedConstructorAnotherClass = test.Test::new;
        qualifiedConstructorAnotherClass.foo();
        MethodReferenceHelperClass.staticFun0(test.Test::new);
        h.memberFun0(test.Test::new);

        JFunction0 constructorAnotherClassWithoutConstructor = Test2::new;
        constructorAnotherClassWithoutConstructor.foo();
        MethodReferenceHelperClass.staticFun0(Test2::new);
        h.memberFun0(Test2::new);
    }

    public void testLibraryFunctions() {
        JFunction2<String, Integer> memberFunFromClass = String::length;
        memberFunFromClass.foo("str");

        new Thread(System.out::println).start();
        ((Runnable) System.out::println).run();
    }

    public void testOverloads() {
        JFunction1<String> constructorWithoutParams = Test::testOverloads;
        constructorWithoutParams.foo();
        MethodReferenceHelperClass.<String>staticFun1(Test::testOverloads);
        h.<String>memberFun1(Test::testOverloads);

        JFunction2<Integer, String> constructorWithParam = Test::testOverloads;
        constructorWithParam.foo(2);
        MethodReferenceHelperClass.<Integer, String>staticFun2(Test::testOverloads);
        h.<Integer, String>memberFun2(Test::testOverloads);
    }

    public void testGenericFunctions() {
        JFunction1<List<String>> emptyList = Collections::emptyList;
        emptyList.foo();
        MethodReferenceHelperClass.<List<String>>staticFun1(Collections::emptyList);
        h.<List<String>>memberFun1(Collections::emptyList);
    }

    public static int staticFun() { return 1; }

    public int memberFun() { return 1; }

    public Java8Class() {}
}