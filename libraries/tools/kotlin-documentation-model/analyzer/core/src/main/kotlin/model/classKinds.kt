package org.jetbrains.dokka.model

interface ClassKind

enum class KotlinClassKindTypes : ClassKind {
    CLASS,
    INTERFACE,
    ENUM_CLASS,
    ENUM_ENTRY,
    ANNOTATION_CLASS,
    OBJECT;
}

enum class JavaClassKindTypes : ClassKind {
    CLASS,
    INTERFACE,
    ENUM_CLASS,
    ENUM_ENTRY,
    ANNOTATION_CLASS;
}
