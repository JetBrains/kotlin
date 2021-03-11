interface OurFace
open class OurClass

fun context() {
    val v = object : OurClass(), OurFace {}
    v<caret>
}

//INFO: <div class='definition'><pre>val <b>v</b>: &lt;anonymous object : <a href="psi_element://OurClass">OurClass</a>, <a href="psi_element://OurFace">OurFace</a>&gt;</pre></div>
