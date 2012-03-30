package demo
open class WindowAdapter() {
open public fun windowClosing() : Unit {
}
}
public class Client() : Frame() {
{
var a : WindowAdapter? = object : WindowAdapter() {
override public fun windowClosing() : Unit {
}
}
addWindowListener(a)
addWindowListener(object : WindowAdapter() {
override public fun windowClosing() : Unit {
}
})
}
}