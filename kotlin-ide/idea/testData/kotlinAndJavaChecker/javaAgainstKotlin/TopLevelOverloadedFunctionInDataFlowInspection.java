package test;

import org.jetbrains.annotations.NotNull;
import test.kotlin.*;

public class TopLevelOverloadedFunctionInDataFlowInspection {
    void other(@NotNull Object some) {
        Object foo = TopLevelOverloadedFunctionInDataFlowInspectionKt.foo(some);
    }
}
