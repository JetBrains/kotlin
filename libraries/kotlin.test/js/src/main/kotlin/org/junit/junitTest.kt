package org.junit

@Deprecated("Use 'Test' from kotlin.test package",
        replaceWith = ReplaceWith("Test", "kotlin.test.Test"),
        level = DeprecationLevel.WARNING)
impl typealias Test = kotlin.test.Test
