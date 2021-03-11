public class Testing {
    public static void test(mockLib.foo.LibClass p) {
        p.getNested().<caret>
    }
}

// EXIST: getValInNested
// EXIST: funInNested