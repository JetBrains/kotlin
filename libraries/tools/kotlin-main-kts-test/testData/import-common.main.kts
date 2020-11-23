
var sharedVar = 2

class SharedClass(val msg: String)

object SharedObject {
    val greeting = "Hi"
}

println("${SharedObject.greeting} from common")
