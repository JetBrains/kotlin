package cases.special

@Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
public class HiddenClass
    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor() {

    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    val hiddenVal = 1

    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    var hiddenVar = 2

    @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
    fun hiddenFun() {}

    public var varWithHiddenAccessors: String = ""
        @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
        get
        @Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
        set
}

@Deprecated("For binary compatibility", level = DeprecationLevel.HIDDEN)
fun hiddenTopLevelFun() {}
