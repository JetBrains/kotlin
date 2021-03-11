// FILE: classFromAnotherPackage.kt
package classFromAnotherPackage

import forTests.MyJavaClass

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

// EXPRESSION: MyJavaClass()
// RESULT: instance of forTests.MyJavaClass(id=ID): LforTests/MyJavaClass;

// EXPRESSION: forTests.MyJavaClass()
// RESULT: instance of forTests.MyJavaClass(id=ID): LforTests/MyJavaClass;

// FILE: forTests/MyJavaClass.java
package forTests;
public class MyJavaClass {}