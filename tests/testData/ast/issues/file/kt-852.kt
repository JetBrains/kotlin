package demo
open class Test() {
open fun test() : String? {
var s1 : String? = ""
var s2 : String? = ""
var s3 : String? = ""
if ((s1?.isEmpty()).sure() && (s2?.isEmpty()).sure())
return "OK"
if ((s1?.isEmpty()).sure() && (s2?.isEmpty()).sure() && (s3?.isEmpty()).sure())
return "OOOK"
return ""
}
}