package demo
open class WindowAdapter() {
public open fun windowClosing() : Unit {
}
}
public class Client() : Frame() {
{
val a = object : WindowAdapter() {
public override fun windowClosing() : Unit {
}
}
addWindowListener(a)
addWindowListener(object : WindowAdapter() {
public override fun windowClosing() : Unit {
}
})
}
}