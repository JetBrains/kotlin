public class Testing {
    void f() {
        facades.Renamed<caret>
    }
}

// EXIST: RenamedNew
// ABSENT: RenamedFileFacadeKt

