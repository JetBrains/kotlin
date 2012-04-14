## Collections API

There are a number of extension functions on [Collection](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html), [Iterator](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Iterator-extensions.html), [Map](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Map-extensions.html) and arrays to allow easy composition collections using familiar combinators like

* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#filter(jet.Function1)">filter()</a>
* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#flatMap(jet.Function1)">flatMap()</a>
* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#map(jet.Function1)">map()</a>
* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#fold(T, jet.Function2)">fold()</a>

Functions on [Collection](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html), [Map](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Map-extensions.html) and arrays are all *eager*, in that methods like <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Collection-extensions.html#map(jet.Function1)">map()</a> will create a complete result List when the method completes.

Functions on [Iterator](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Iterator-extensions.html) are *lazy* so that they try to return new iterators that lazily evaluate things. For example <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/java/util/Iterator-extensions.html#map(jet.Function1)">map()</a> returns a new Iterator that lazily maps the values in the original iterator.

## Preconditions

When writing code it is recommended you add checks early in a function to ensure parameters are valid. There are a number of helper methods for this:

* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/package-summary.html#assert(jet.Boolean)">assert(Boolean)</a> and <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/package-summary.html#assert(jet.Boolean, jet.Function0)">assert(Boolean) { lazyMessage }</a> which use the JDK's assertion feature so they can be disabled using the -ea option
* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/package-summary.html#check(jet.Boolean)">check(Boolean)</a> and <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/package-summary.html#check(jet.Boolean, jet.Function0)">check(Boolean) { lazyMessage }</a> for checking something to be true and throwing [IllegalStateException](http://docs.oracle.com/javase/6/docs/api/java/lang/IllegalStateException.html) if not
* <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/package-summary.html#require(jet.Boolean)">require(Boolean)</a> and <a href="http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/package-summary.html#require(jet.Boolean, jet.Function0)">require(Boolean) { lazyMessage }</a> for requiring something to be true and throwing [IllegalArgumentException](http://docs.oracle.com/javase/6/docs/api/java/lang/IllegalArgumentException.html) if not
