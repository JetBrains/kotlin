package demo
class WindowAdapter() {
public open fun windowClosing() {
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