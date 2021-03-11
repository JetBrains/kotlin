// FILE: jcImports.kt
package jcImports

fun main(args: Array<String>) {
    val javaClass = forTests.javaContext.JavaClass()
    //Breakpoint!
    javaClass.imports()
}

// STEP_INTO: 1
// STEP_OVER: 1

// EXPRESSION: list.filter { it == 1 }.size
// RESULT: 1: I

// FILE: forTests/javaContext/JavaClass.java
package forTests.javaContext;

import java.util.ArrayList;

public class JavaClass {
    public void imports() {
        ArrayList<Integer> list = createList();
        int breakpoint = 1;
    }

    private ArrayList<Integer> createList() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        return list;
    }
}