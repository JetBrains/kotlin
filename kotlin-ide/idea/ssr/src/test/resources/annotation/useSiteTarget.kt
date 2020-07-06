@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class Ann

<warning descr="SSR">class One(@get:Ann val prop: String)</warning>

class Two(@Ann val prop: String)