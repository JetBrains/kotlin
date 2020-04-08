interface FooBar1

interface FooBar2 {
    val list: List<FooBar<caret>1>
}

// EXIST: FooBar1
// EXIST: FooBar2
