import k.PlatformStaticFun;

public class TestPlatformStaticFun {
    public static void foo() {
        PlatformStaticFun.<caret>test();
    }
}

// REF: (in k.PlatformStaticFun).test()
// CLS_REF: (in k.PlatformStaticFun).test()