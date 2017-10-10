package org.junit

@Suppress("NO_ACTUAL_FOR_EXPECT")
@Deprecated("Use 'Test' from kotlin.test package",
            replaceWith = ReplaceWith("kotlin.test.Test", "kotlin.test.Test"),
            level = DeprecationLevel.WARNING)
expect annotation class Test()
