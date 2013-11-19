package demo
open class WindowAdapter() {
public open fun windowClosing() {
}
}
public class Client() : Frame() {
{
var a : WindowAdapter? = object : WindowAdapter() {
public override fun windowClosing() {
}
}
addWindowListener(a)
addWindowListener(object : WindowAdapter() {
public override fun windowClosing() {
}
})
}
}