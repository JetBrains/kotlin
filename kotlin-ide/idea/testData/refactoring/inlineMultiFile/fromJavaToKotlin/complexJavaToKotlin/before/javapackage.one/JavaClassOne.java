package javapackage.one;

import usage.KotlinClassOne;
import usage.KotlinClassTwo;

public class JavaClassOne {
    public JavaClassOne apply(KotlinClassOne kotlinClassOne, KotlinClassTwo two) {
        KotlinClassOne kotlinOther = new KotlinClassOne();

        kotlinClassOne.update(this);
        JavaClassOne result = JavaClassOne.build(kotlinOther.hashCode());
        kotlinOther.update(result);
        System.out.println(two);

        System.out.println(kotlinClassOne);
        System.err.println(result);
        return build(result.convertToInt() + hashCode() + convertToInt() + MAGIC_CONST + build(field).field);
    }

    public int field = MAGIC_CONST;

    public int convertToInt() {
        return 42;
    }

    public static JavaClassOne build(int number) {
        return new JavaClassOne();
    }

    public static int MAGIC_CONST = 42;
}