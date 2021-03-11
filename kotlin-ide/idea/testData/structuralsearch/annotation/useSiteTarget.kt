@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FIELD)
annotation class Ann

<warning descr="SSR">class One(@get:Ann val prop: String)</warning>

class Two(@field:Ann val prop: String)

class Three(@Ann val prop: String)