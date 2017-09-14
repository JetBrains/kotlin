annotation class MyAnnotation

class Test1(@MyAnnotation var bar: Int)

class Test2(@get:MyAnnotation @set:MyAnnotation @setparam:MyAnnotation @property:MyAnnotation @field:MyAnnotation var bar: Int)