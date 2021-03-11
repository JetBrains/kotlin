// "Replace invalid positioned arguments for annotation" "true"
// WITH_RUNTIME
// ERROR: Only named arguments are available for Java annotations
// ERROR: The integer literal does not conform to the expected type String

@Ann(1, 2<caret>) class A
