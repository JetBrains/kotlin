@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun foo(arg: String?) {
    (@Ann arg)<caret>!!
}