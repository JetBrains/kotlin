package javaApi;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import kotlinApi.KotlinClassWithProperties;

public @interface Anon1 {
    String[] value();
    String[] stringArray();
    int[] intArray();
    String string();
}

public @interface Anon2 {
    String value();
    int intValue();
    char charValue();
}

public @interface Anon3 {
    E e();
    String[] stringArray();
    String[] value();
}

public @interface Anon4 {
    String[] value();
}

public @interface Anon5 {
    int value();
}

public @interface Anon6 {
    String[] value();
    int intValue() default 10;
}

public @interface Anon7 {
    Class[] value();
}

public @interface Anon8 {
    Class[] classes();
}

public enum E {
    A, B, C
}

public class Base {
    public @Nullable String foo(@Nullable String s) { return s; }

    public int getProperty() { return 1; }
    public void setProperty(int value) {}
}

public class Derived extends Base {
    public String foo(String s) { return s; }
}

public class WithVarargConstructor {
    public WithVarargConstructor(int p, Object... objects) { }
}

public class T {
    public Set<String> set;
}

public interface JFunction0 {
    void foo();
}

public interface Listener {
    public void onChange(int visibility);
}

public interface JFunction1ReturnType<T> {
    void foo(T t);
}

public interface JFunction1<T> {
    T foo();
}

public interface JFunction2<T, K> {
    K foo(T p);
}

public class MethodReferenceHelperClass {
    public static void staticFun0(JFunction0 f) {}
    public static <T> void staticFun1(JFunction1<T> f) {}
    public static <T, K> void staticFun2(JFunction2<T, K> f) {}

    public void memberFun0(JFunction0 f) {}
    public <T> void memberFun1(JFunction1<T> f) {}
    public <T, K> void memberFun2(JFunction2<T, K> f) {}
}

public class JavaClassWithProperties {
    public int getValue1() { return 1; }

    public int getValue2() { return 1; }
    public void setValue2(int value) { }

    public int getValue3() { return 1; }
    public void setValue3(int value) { }

    public int getValue4() { return 1; }
    public void setValue4(int value) { }
}

public class JavaClassDerivedFromKotlinClassWithProperties extends KotlinClassWithProperties {
    @Override
    public String getSomeVar1() { return ""; }

    @Override
    public void setSomeVar2(String value) { }
}

public class JavaClass {
    public int get(int p) {
        return 0;
    }
}