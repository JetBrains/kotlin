import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___meaningOfLife")
public fun __root___meaningOfLife(): Int {
    val _result = meaningOfLife()
    return _result
}

private inline fun <reified T, reified U> T.autoCast(): U = this as U
