package test

annotation class Ann1
annotation class Ann2

@Ann1
@Ann2
class AnnotationListBecomeNotEmpty

class AnnotationListBecomeEmpty

@Ann1
@Ann2
class AnnotationAdded

@Ann1
class AnnotationRemoved

@Ann2
class AnnotationReplaced
