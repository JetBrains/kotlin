package javaApi;

import org.jetbrains.annotations.Nullable;

import java.lang.Override;
import java.lang.String;

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
}

public class Derived extends Base {
    public String foo(String s) { return s; }
}

public class WithVarargConstructor {
    public WithVarargConstructor(int p, Object... objects) { }
}
