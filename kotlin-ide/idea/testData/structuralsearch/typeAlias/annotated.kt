@Target(AnnotationTarget.TYPEALIAS)
annotation class Ann

<warning descr="SSR">@Ann typealias aliasOne = List<String></warning>
typealias aliasTwo = List<Int>