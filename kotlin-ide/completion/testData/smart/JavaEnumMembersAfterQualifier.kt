import java.lang.annotation.ElementType

fun foo(){
    val e : ElementType = ElementType.<caret>
}

// EXIST: { lookupString:"TYPE", itemText:"TYPE", tailText:" (java.lang.annotation.ElementType)" }
// EXIST: { lookupString:"FIELD", itemText:"FIELD", tailText:" (java.lang.annotation.ElementType)" }
