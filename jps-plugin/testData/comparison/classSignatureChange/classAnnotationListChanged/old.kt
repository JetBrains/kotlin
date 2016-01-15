package test

annotation class Ann1
annotation class Ann2

class AnnotationListBecomeNotEmpty

@Ann1
@Ann2
class AnnotationListBecomeEmpty

@Ann1
class AnnotationAdded

@Ann1
@Ann2
class AnnotationRemoved

@Ann1
class AnnotationReplaced
