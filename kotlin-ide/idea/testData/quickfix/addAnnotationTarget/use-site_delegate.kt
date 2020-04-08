// "Add annotation target" "true"
// WITH_RUNTIME
@Target
annotation class DelegateAnn

<caret>@delegate:DelegateAnn
val foo by lazy { "" }