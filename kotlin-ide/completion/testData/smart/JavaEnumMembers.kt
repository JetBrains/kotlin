import java.lang.annotation.ElementType

fun foo(){
    val e : ElementType = <caret>
}

// EXIST: { lookupString:"TYPE", itemText:"ElementType.TYPE", tailText:" (java.lang.annotation)", typeText:"ElementType" }
// EXIST: { lookupString:"FIELD", itemText:"ElementType.FIELD", tailText:" (java.lang.annotation)", typeText:"ElementType" }
