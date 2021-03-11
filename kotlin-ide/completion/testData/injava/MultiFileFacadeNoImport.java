package test;

public class Testing {
    void f() {
        MultiFile<caret>
    }
}

// EXIST: MultiFileFacadeClass
// NUMBER: 1
// INVOCATION_COUNT: 2
