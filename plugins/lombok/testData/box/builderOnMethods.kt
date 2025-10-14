// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-71893

// FILE: User.java

import lombok.Builder;

public class User {
    public String name;
    public int age;

    @Builder
    public static User create(String name, int age) {
        return new User(name, age);
    }

    // This builder is not generated for this method because it has the name that's overlapped by the first method's name
    @Builder
    public static User create(String name) {
        return new User(name, -1);
    }

    private User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: User2.java

import lombok.Builder;

public class User2 {
    public String name;
    public int age;

    @Builder
    public void init(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Builder(builderClassName = "MyBuilder", builderMethodName = "myBuilder", buildMethodName = "myBuild")
    public void init2(String name, int age) {
        init("My" + name, age + 20);
    }
}

// FILE: ReturnType.java

import lombok.Builder;

public class ReturnType {
    public static class camelCaseKlass {
        public String value = "OK";
    }

    @Builder(builderMethodName = "intBuilder")
    int initWithInt(int i) {
        return i;
    }

    @Builder(builderMethodName = "integerBuilder")
    Integer initWithInteger(Integer i) {
        return i;
    }

    @Builder(builderMethodName = "stringBuilder")
    String initWithString(String s) {
        return s;
    }

    @Builder(builderMethodName = "klassBuilder")
    camelCaseKlass initWithCustomClass(camelCaseKlass klass) {
        return klass;
    }
}

// FILE: ReturnType2.java

import lombok.Builder;

public class ReturnType2 {
    public static class camelCaseKlass {
        public String value = "OK";
    }

    @Builder(builderMethodName = "builderOnStaticMethodWithCustomReturnType")
    static camelCaseKlass staticMethodWithCustomReturnType(camelCaseKlass klass) {
        return klass;
    }
}

// FILE: test.kt

fun box(): String {
    // Check Builder on a static method
    val userBuilder: User.UserBuilder = User.builder()
    val user: User = userBuilder.name("John").age(42).build()
    if (user.name != "John" || user.age != 42) {
        return "Error: $user"
    }

    // Check Builder on an nonstatic method
    val user2 = User2()
    // Default builder name for nonstatic methods differs from static ones and it's inferred from the return type of the corresponding method
    val user2Builder: User2.VoidBuilder = user2.builder()
    user2Builder.age(24).name("Alex").build()
    if (user2.name != "Alex" || user2.age != 24) {
        return "Error: $user2"
    }

    // Check Builder on an nonstatic method with a specified names
    val myBuilder: User2.MyBuilder = user2.myBuilder()
    myBuilder.age(0).name("Yulia").myBuild()
    if (user2.name != "MyYulia" || user2.age != 20) {
        return "Error: $user2"
    }

    // Check Builder on nonstatic and nonvoid methods
    val returnType = ReturnType()

    val intBuilder: ReturnType.IntBuilder = returnType.intBuilder()
    if (42 != intBuilder.i(42).build()) {
        return "FAIL (intBuilder)"
    }

    val integerBuilder: ReturnType.IntegerBuilder = returnType.integerBuilder()
    if (100 != integerBuilder.i(100).build()) {
        return "FAIL (integerBuilder)"
    }

    val stringBuilder: ReturnType.StringBuilder  = returnType.stringBuilder()
    if ("OK" != stringBuilder.s("OK").build()) {
        return "FAIL (stringBuilder)"
    }

    val klassBuilder: ReturnType.camelCaseKlassBuilder = returnType.klassBuilder()
    if ("OK" != klassBuilder.klass(ReturnType.camelCaseKlass()).build().value) {
        return "FAIL (klassBuilder)"
    }

    val builderOnStaticMethodWithCustomReturnType: ReturnType2.camelCaseKlassBuilder =
        ReturnType2.builderOnStaticMethodWithCustomReturnType()
    if ("OK" != builderOnStaticMethodWithCustomReturnType.klass(ReturnType2.camelCaseKlass()).build().value) {
        return "FAIL (builder on static method with custom return type)"
    }

    return "OK"
}
