package unexported

class Klass
interface Interface

typealias TypeAliasToPublic = String
typealias TypeAliasToUnexported = Klass

fun function() {}

val valProperty = ""
var varProperty = ""
