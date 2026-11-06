# Assert whole values in Kotlin Gradle plugin tests

Applies to:
```
**/src/test/**
**/src/functionalTest/**
!**/src/test/resources/**
!**/src/functionalTest/resources/**
```

In Kotlin Gradle plugin tests, make assertion failures show the actual values.
When a test checks a value against an expected one, compare the whole value with `assertEquals`
(on `.prettyPrinted` for collections and other structures, on the full content for files the plugin generates
when that content is what the test checks and has no versions, paths or timestamps)
instead of asserting a derived boolean, such as `assertTrue(list.isEmpty())`, `assertTrue(x in list)`,
`assertFalse(list.any { it.startsWith(prefix) })` or `assertTrue(actual == expected)`,
checking elements or fields one by one, or checking fragments.

These are fine:
- `assertTrue(condition)` for a boolean condition with no value worth showing, such as `list.isNotEmpty()`,
  whose failure can only mean an empty list; don't rewrite it as `assertEquals(true, condition)`;
- `assertFalse(x in list)` or `assertFalse(list.any { it.name == name })` for one exact element,
  whose failure can only mean that this element is there (unlike a pattern, such as a prefix, where the failure
  should show which element matched);
- `assertTrue` or `assertFalse` with a message that includes the actual value;
- checking a fragment with a helper that shows the actual value on failure,
  e.g. `assertContains` or `assertHasDiagnostic(..., withSubstring = ...)`;
- checking fragments of content the plugin doesn't generate itself, e.g. output of external tools or npm templates
  (also when the plugin patches them);
- an `assertEquals` that already compares the whole value without `.prettyPrinted`.

Don't use `assert` from the `kotlin` package: it works only when assertions are enabled with `-ea`.
