public class Method {
    public static void foo() {
        (new k.Class()).f<caret>unction();
    }
}

// REF: (in k.Class).function()
// CLS_REF: (in k.Class).function()
