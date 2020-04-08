fun foo(){
    val l : java.util.Calendar = <caret>
}

// EXIST: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"() (java.util)", typeText:"Calendar!", attributes:"" }
// EXIST: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"(aLocale: Locale!) (java.util)", typeText:"Calendar!", attributes:"" }
// EXIST: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"(zone: TimeZone!, aLocale: Locale!) (java.util)", typeText:"Calendar!", attributes:"" }
