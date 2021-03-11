package a

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class FileAnnotation()

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ClassAnnotation()

class A

fun a() {
}
