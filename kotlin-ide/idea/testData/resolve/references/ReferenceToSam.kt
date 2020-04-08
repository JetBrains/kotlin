fun some() {
    val jClass = JavaTest.SomeJavaClass()
    jClass.<caret>setListener {}
}

// REF: (in JavaTest.SomeJavaClass).setListener(SAMInterface)
