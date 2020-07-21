fun JavaClass.foo(javaClass: JavaClass) {
    print(javaClass.<caret>something)
    javaClass.<caret>something = 1
    javaClass.<caret>something += 1
    javaClass.<caret>something++
    --javaClass.<caret>something

    <caret>something++
    (<caret>something)++
    (<caret>something) = 1
    (javaClass.<caret>something) = 1
}

// MULTIRESOLVE
// REF1: (in JavaClass).getSomething()
// REF2: (in JavaClass).setSomething(int)
// REF3: (in JavaClass).getSomething()
// REF3: (in JavaClass).setSomething(int)
// REF4: (in JavaClass).getSomething()
// REF4: (in JavaClass).setSomething(int)
// REF5: (in JavaClass).getSomething()
// REF5: (in JavaClass).setSomething(int)
// REF6: (in JavaClass).getSomething()
// REF6: (in JavaClass).setSomething(int)
// REF7: (in JavaClass).getSomething()
// REF7: (in JavaClass).setSomething(int)
// REF8: (in JavaClass).setSomething(int)
// REF9: (in JavaClass).setSomething(int)
