package demo
open class Test() {
open fun test() : String? {
var s1 : String? = ""
var s2 : String? = ""
var s3 : String? = ""
if (s1?.isEmpty()!! && s2?.isEmpty()!!)
return "OK"
if (s1?.isEmpty()!! && s2?.isEmpty()!! && s3?.isEmpty()!!)
return "OOOK"
return ""
}
}