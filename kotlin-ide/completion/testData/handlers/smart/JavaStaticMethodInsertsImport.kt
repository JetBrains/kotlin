fun foo(){
    val l : java.util.Calendar = <caret>
}

// ELEMENT_TEXT: "Calendar.getInstance"
// TAIL_TEXT: "(TimeZone!) (java.util)"
