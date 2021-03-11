class Foo {
    <caret>lateinit var foo: String
}

//INFO: 'lateinit' allows initializing a <a href="https://kotlinlang.org/docs/reference/properties.html#late-initialized-properties-and-variables">not-null property outside of a constructor</a>