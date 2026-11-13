// ISSUE: KT-91596
// Needs a javac with value classes (JDK 28) and preview features enabled
// MIN_JAVA_VERSION: 28
// JAVAC_OPTIONS: --enable-preview=
// VALHALLA_VALUE_CLASSES
// ENABLE_JVM_PREVIEW
// JVM_TARGET: 28
// LANGUAGE: +FullValueClasses

value class Money(val cents: Long, val currency: String)
