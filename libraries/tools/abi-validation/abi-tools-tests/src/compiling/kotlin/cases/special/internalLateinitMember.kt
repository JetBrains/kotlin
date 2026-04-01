package cases.special

public class ClassWithLateInitMembers internal constructor() {

    public lateinit var publicLateInitWithInternalSet: String
        internal set

    internal lateinit var internalLateInit: String

}