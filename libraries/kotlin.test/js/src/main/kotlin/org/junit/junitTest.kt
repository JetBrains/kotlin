package org.junit

@Deprecated("Use 'Test' from kotlin.test package",
        replaceWith = ReplaceWith("kotlin.test.Test", "kotlin.test.Test"),
        level = DeprecationLevel.WARNING)
actual typealias Test = kotlin.test.Test
