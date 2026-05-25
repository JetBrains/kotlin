interface NewProfileOBViewModel {
    fun overrideMe(): @Type () -> Unit
}

class ReturningProfileObViewModel : NewProfileOBViewModel {
    override fun overrideMe(): @Type () -> Unit = {}
}

@Target(AnnotationTarget.TYPE)
annotation class Type
