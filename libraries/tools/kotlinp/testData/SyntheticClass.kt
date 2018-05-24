fun f(a: AnnotationTarget): String? {
    return when (a) {
        AnnotationTarget.FUNCTION -> "1"
        AnnotationTarget.PROPERTY -> "2"
        else -> null
    }
}
