annotation class MyAnnotation
annotation class MyAnnotation2
annotation class MyAnnotation3
annotation class MyAnnotation4
annotation class MyAnnotation5

class Test1(@MyAnnotation var bar: Int)

class Test2(@get:MyAnnotation @set:MyAnnotation2 @setparam:MyAnnotation3 @property:MyAnnotation4 @field:MyAnnotation5 var bar: Int)
