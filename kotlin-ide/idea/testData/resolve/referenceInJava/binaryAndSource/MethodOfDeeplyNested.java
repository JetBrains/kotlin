public class MethodOfDeeplyNested {
    public static void foo() {
        (new k.Class.F.F()).f<caret>unction();
    }
}

// REF: (in k.Class.F.F).function()
// CLS_REF: (in k.Class.F.F).function()
