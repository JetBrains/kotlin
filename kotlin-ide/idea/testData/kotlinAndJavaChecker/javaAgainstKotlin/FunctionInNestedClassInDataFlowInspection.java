package test;

import org.jetbrains.annotations.NotNull;
import test.kotlin.A.B.C.C;

public class FunctionInNestedClassInDataFlowInspection {
    void other(@NotNull Object some) {
        Object foo = new C().foo(some);
    }
}

