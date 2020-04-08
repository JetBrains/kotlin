public class Field {
    public static void foo() {
        (new k.Class()).p<caret>rop;
    }
}

// REF: (in k.Class).prop
// CLS_REF: (in k.Class).prop
