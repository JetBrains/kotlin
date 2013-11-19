package demo
class WindowAdapter() {
public fun windowClosing() {
}
}
public class Client() : Frame() {
{
val a = object : WindowAdapter() {
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