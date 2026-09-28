/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.sample

/**
 * Represents a sample code snippet of a Kotlin function. The snippet includes both file
 * import directives (all, even unused) and the sample function body.
 *
 * @property imports list of import statement values, without the `import` prefix.
 *                   Contains no blank lines. Example of a single value: `com.example.pckg.MyClass.function`.
 * @property body body of the sample function, without the function name or curly braces, only the inner body.
 *                Common minimal indent of all lines is trimmed. Leading and trailing line breaks are removed.
 *                Trailing whitespaces are removed. Example: given the sample function `fun foo() { println("foo") }`,
 *                the sample body will be `println("foo")`.
 *
 * @see SampleAnalysisEnvironment for how to acquire it
 */
public class SampleSnippet(
    public val imports: List<String>,
    public val body: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SampleSnippet

        if (imports != other.imports) return false
        if (body != other.body) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imports.hashCode()
        result = 31 * result + body.hashCode()
        return result
    }

    override fun toString(): String {
        return "SampleSnippet(imports=$imports, body='$body')"
    }
}
