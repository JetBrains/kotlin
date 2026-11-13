// Semantics gate for the stable-cell class encoding (-Xwasm-hot-reload):
// compiled WITHOUT the flag and WITH it, both must print identical output.
// Exercises inherited fields, virtual dispatch, mutation, `is`/`as`, and
// the cell+payload path (extract -> version cast -> field) everywhere.
//
// build (dist kotlinc; the wasm needs the klib step + link step like lrepl):
//   kotlinc-wasm -Xwasm-target=wasm-wasi cell_encoding_check.kt \
//     -ir-output-dir . -ir-output-name <name> -libraries <stdlib-wasi.klib> \
//     [-Xwasm-hot-reload]                       # <- the only difference
//   ...then the same with -Xinclude=<name>.klib -main noCall for the link
// run: node driver_cells.mjs <name>.wasm
open class Base(val x: Int) { fun getX() = x }
class Child(val y: Int, x: Int) : Base(x) { fun sum() = x + y; fun base() = getX() + y }
class Mut { var n = 0; fun bump(): Int { n = n + 1; return n } }
fun main() {
    val c = Child(2, 3)
    println("sum=${c.sum()} base=${c.base()} x=${c.getX()}")
    val m = Mut()
    m.bump(); m.bump()
    println("n=${m.n}")
    val box: Any = c
    println("isBase=${box is Base} isChild=${box is Child} isMut=${box is Mut}")
    println("cast=${(box as Base).getX()}")
}
