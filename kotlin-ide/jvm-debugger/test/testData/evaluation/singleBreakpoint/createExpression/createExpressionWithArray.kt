// FILE: createExpressionWithArray.kt
package createExpressionWithArray

import forTests.MyJavaClass
// do not remove this import, it checks that we do not insert ambiguous imports during EE
import forTests.MyJavaClass.InnerClass

fun main(args: Array<String>) {
    val baseArray = arrayOf(MyJavaClass().getBaseClassValue())
    val innerArray = arrayOf(MyJavaClass().getInnerClassValue())
    //Breakpoint!
    val a = 1
}

// PRINT_FRAME
// DESCRIPTOR_VIEW_OPTIONS: NAME_EXPRESSION_RESULT

// FILE: forTests/MyJavaClass.java
package forTests;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public class MyJavaClass {
    public static class BaseClass {
        public final int i2 = 1;
    }

    public BaseClass getBaseClassValue() {
        return new BaseClass();
    }
    public BaseClass getInnerClassValue() {
        return new InnerClass();
    }

    public static class InnerClass extends BaseClass {
        public final int i = 1;
    }

    public MyJavaClass() {}
}
