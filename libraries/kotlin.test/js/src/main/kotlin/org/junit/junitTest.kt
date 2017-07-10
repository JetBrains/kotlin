package org.junit

@Suppress("IMPLEMENTATION_WITHOUT_HEADER")
@Deprecated("Use 'Test' from kotlin.test package",
        replaceWith = ReplaceWith("Test", "kotlin.test.Test"),
        level = DeprecationLevel.WARNING)
impl annotation class Test(val name: String = "")
