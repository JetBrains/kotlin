package cases.special

@JvmName("internalFun")
internal fun internalRenamedFun() {}

internal var internalVar: Int = 1
    @JvmName("internalVarGetter")
    get
    @JvmName("internalVarSetter")
    set

@JvmName("publicFun")
public fun publicRenamedFun() {}

public var publicVar: Int = 1
    @JvmName("publicVarGetter")
    get
    @JvmName("publicVarSetter")
    set



