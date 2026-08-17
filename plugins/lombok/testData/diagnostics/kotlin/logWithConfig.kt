// FULL_JDK

// FILE: test.kt

import lombok.extern.java.Log

<!LOG_PROPERTY_ALREADY_EXISTS!>@Log<!>
class LogExampleWithDirectLogField {
    val log = "No log"
}

@Log // No warning
class LogAndExtensionProperty

val LogAndExtensionProperty.log: String
    get() = "No log"

// No warning despite the confusing extension and contextual properties that actually don't conflict with the property being generated.
@Log
class LogWhenNonConflictingExtensionAndContextualProperty {
    val LogWhenNonConflictingExtensionAndContextualProperty.<!EXTENSION_SHADOWED_BY_MEMBER!>log<!>: Int get() = 1

    context(p: LogWhenNonConflictingExtensionAndContextualProperty)
    val log: LogWhenNonConflictingExtensionAndContextualProperty get() = p
}

// A companion object is not a target whatever `lombok.log.fieldIsStatic` says. With `false` the annotation used to
// be dropped silently - neither generated nor reported - which is what KT-88288 reports.
class LogOnCompanion {
    <!ANNOTATION_HAS_NO_EFFECT!>@Log<!>
    companion object
}

// FILE: lombok.config

lombok.log.fieldIsStatic=false
