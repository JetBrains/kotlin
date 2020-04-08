public class Testing {
    void f(mockLib.foo.LibTrait p) {
        p.<caret>
    }
}

// EXIST: foo
// LIGHT_CLASS: mockLib.foo.LibTrait