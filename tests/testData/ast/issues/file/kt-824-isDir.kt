package test
import java.io.File
/**
* User: ignatov
*/
public open class Test() {
class object {
public open fun isDir(parent : File?) : Boolean {
if (parent == null || !parent?.exists()!!)
{
return false
}
var result : Boolean = true
if (parent?.isDirectory()!!)
{
return true
}
else
return false
}
}
}