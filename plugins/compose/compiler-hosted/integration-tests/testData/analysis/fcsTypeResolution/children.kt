// RUN_PIPELINE_TILL: FRONTEND

                import androidx.compose.runtime.*

@Composable fun ChildrenRequired2(content: @Composable () -> Unit) { content() }

@Composable fun ChildrenOptional3(content: @Composable () -> Unit = {}){ content() }

@Composable fun NoChildren2() {}

@Composable
fun MultiChildren(c: @Composable (x: Int) -> Unit = {}) { c(1) }

@Composable
fun MultiChildren(c: @Composable (x: Int, y: Int) -> Unit = { x, y ->println(x + y) }) { c(1,1) }


                @Composable fun Test() {
                    ChildrenRequired2 {}
                    <!NO_VALUE_FOR_PARAMETER!>ChildrenRequired2<!>()

                    ChildrenOptional3 {}
                    ChildrenOptional3()

                    NoChildren2 <!TOO_MANY_ARGUMENTS!>{}<!>
                    NoChildren2()

                    // This call is not ambiguous in K2. The call can only match the single
                    // argument lambda - with an implicit `it`. The two argument version would
                    // have required explicit lambda parameters.
                    MultiChildren {}
                    MultiChildren { x ->
                        println(x)
                    }
                    MultiChildren { x, y ->
                        println(x + y)
                    }
                    <!NONE_APPLICABLE!>MultiChildren<!> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>y<!>, <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>z<!> ->
                        println(x + y + z)
                    }
                }
