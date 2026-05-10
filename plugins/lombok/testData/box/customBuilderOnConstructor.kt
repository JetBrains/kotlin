// ISSUE: KT-71547

// FILE: User.java

import lombok.Builder;
import lombok.Data;

@Data
public class User {
    private String name;
    private int age;

    @Builder(builderClassName = "ConstructorBuilder", builderMethodName = "constructorBuilder", buildMethodName = "constructorBuild")
    public User(String userName) {
        this.name = userName;
        this.age = -1;
    }

    @Builder(builderClassName = "ConstructorBuilder2", builderMethodName = "constructorBuilder2", buildMethodName = "constructorBuild2")
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val constructorBuilder = User.constructorBuilder();
    val user = constructorBuilder.userName("Brian").constructorBuild();

    assertEquals("Brian", user.name)

    val constructorBuilder2 = User.constructorBuilder2();
    val user2 = constructorBuilder2.name("John").age(42).constructorBuild2();

    assertEquals("John", user2.name)
    assertEquals(42, user2.age)
    return "OK"
}
