// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-58695

// FILE: MyClass.java

import lombok.Builder;

@Builder(toBuilder = true, builderClassName = "MyClassBuilder")
public class MyClass {
    public String aString;

    public static MyClassBuilder builder(int x) {
        return new CustomMyClassBuilder();
    }

    public static class MyClassBuilder {
        static int myStaticField = 42;
    }

    // Make sure Lombok ignores the custom unrelated class
    public static class MyCustomClass {
    }

    public static class CustomMyClassBuilder extends MyClassBuilder {
        @Override
        public MyClass build() {
            myStaticField = 100;
            return super.build();
        }
    }
}

// FILE: test.kt

fun box(): String {
    MyClass.MyClassBuilder.myStaticField = 84

    val myClassBuilder: MyClass.MyClassBuilder = MyClass.builder(0)
    val myClass = myClassBuilder.aString("test").build()

    return if (myClassBuilder is MyClass.CustomMyClassBuilder && // Check if custom `builder` method is called
        MyClass.MyClassBuilder.myStaticField == 100 && // Check if custom `build` method is called
        myClass.aString == "test"
     ) {
        "OK"
    } else {
        "Error: $myClass"
    }
}
