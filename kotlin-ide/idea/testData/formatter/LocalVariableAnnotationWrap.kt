fun foo() {
    @Deprecated @Named("foo") val bar = 1
}

// SET_INT: VARIABLE_ANNOTATION_WRAP = 2
