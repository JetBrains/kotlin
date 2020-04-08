fun main(args: Array<String>){
    val x = "abc"
    val y = "cde"
    val z = "$y" +<caret> "$x.bar"
}
