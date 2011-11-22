namespace demo {
import java.awt.*
import java.awt.event.*
import java.io.*
import java.net.*
public class Client() : Frame() {
{
var a : WindowAdapter? = object : WindowAdapter() {
open public fun windowClosing(e : WindowEvent?) : Unit {
dispose()
}
}
addWindowListener(a)
addWindowListener(object : WindowAdapter() {
open public fun windowClosing(e : WindowEvent?) : Unit {
dispose()
}
})
}
}
}