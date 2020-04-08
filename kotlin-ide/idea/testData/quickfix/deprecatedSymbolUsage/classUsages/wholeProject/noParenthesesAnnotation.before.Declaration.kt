package dependency

@Deprecated("", ReplaceWith("dependency.NewAnnotation"))
annotation class OldAnnotation

annotation class NewAnnotation()

@OldAnnotation
class C