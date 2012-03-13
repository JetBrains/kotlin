## Warning experimental APIs

This package contains a couple of alternative experimental implementations of string templates.

These are not yet integrated into the language, but are here to compare implementation details,
suitability, code size, complexity and efficiency.

### Aims

* make it easy to generate various kinds of output such as text, text with internationalisation, HTML/XML, JSON, URL/URLs or make JDBC calls while reusing the same language templates with $ expressions

### Issues

* how many objects are created per call?
* how extensible & easy to reuse the concept of String Templates in other library features
* pass context into the template somehow; so that formatting options (Locales, nullText, JDBC Connection, converters or whatnot) can be customized
* avoid where possible lots of "if (value is SomeType)" runtime checks to determine what encoding strategy should be used


#### experiment1

Here we convert a string template into a function on some kind of builder object where we invoke the text() method for static text and expression() for dynamic expressions

* [example](https://github.com/JetBrains/kotlin/blob/master/libraries/testlib/test/template/experiment1/HtmlTemplateTest.kt#L17)

Pro:

* simple and efficient code is generated - no arrays
* no arrays are created for static and expression arguments
* static dispatch of encoding functions; no runtime instanceof checks on each argument

Cons:

* function may create an inner object/class (though the compiler may get smart enough to inline some of those - or maybe generate a custom function for the whole thing with parameters?)
* we can only use this strategy if the template expression is passed to a function and we can detect the function parameter takes a fn: (T) -> Unit; and T has suitable text() and expression() methods.
 (We can use extension methods to allow regular builder-like classes such as java.lang.StringBuilder to be used to minimise redundant object creation) - so the code generation is a bit more complex

#### experiment2

Here we create a single StringTemplate object containing the constant text array and the values array. Then we use functions or extension functions as ways to do other things.

* [example](https://github.com/JetBrains/kotlin/blob/master/libraries/testlib/test/template/experiment2/HtmlTemplateTest.kt#L13)

Pros:

* really simple
* no real compiler magic required where the type depends on the context

Cons:

* requires runtime [instanceof checks](https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/src/kotlin/template/experiment2/Templates.kt#L66) on each expression value
* fair bit of object construction per call (2 arrays, a template object, then the string builder and lots of index lookup of arrays


#### experiment 3

The idea is we create a FooTemplate object with the static constant strings inside; so we can easily cache the immutable stuff on startup. Then we basically use pseudo code like

    val template = StringTemplate("some ", " static ", "text")
    ...
    val builder = template.builder()
    builder.expression(foo)
    builder.expression(bar)
    val answer = builder.build()

Then based on the context - e.g. an annotation or a method parameter type or something; we determine which kind of template to create (StringTemplate, HtmlTemplate, JdbcTemplate etc)

Pros:

* fairly simple

Cons:

* quite a few different types (FooTemplate and FooBuilder)
* no way to pass in parameters to the FooBuilder class; for example the JDBC connection, or the Locale / NumberFormats to use for numeric formatting etc
* requires runtime instanceof checks on each expression value & array access etc
