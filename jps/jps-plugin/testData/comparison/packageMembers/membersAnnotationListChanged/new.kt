package test

annotation class Ann1
annotation class Ann2

@Ann1
@Ann2
fun annotationListBecameNotEmpty() {}

fun annotationListBecameEmpty() {}

@Ann1
@Ann2
fun annotationAdded() {}

@Ann1
fun annotationRemoved() {}

@Ann2
fun annotationReplaced() {}
