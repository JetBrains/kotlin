fun foo(): java.util.Calendar = Ca<caret>

// EXIST_JAVA_ONLY: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"() (java.util)", typeText:"Calendar!" }
// EXIST_JAVA_ONLY: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"(zone: TimeZone!) (java.util)", typeText:"Calendar!" }
// EXIST_JAVA_ONLY: { lookupString:"getInstance", itemText:"Calendar.getInstance", tailText:"(zone: TimeZone!, aLocale: Locale!) (java.util)", typeText:"Calendar!" }
