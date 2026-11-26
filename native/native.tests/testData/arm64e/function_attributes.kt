// CHECK: define {{.*}}@"kfun:{{.*}}foo(){}kotlin.Int"() #[[ATTRIBUTES:[0-9]+]]
// CHECK: attributes #[[ATTRIBUTES]] = {{.*}}"ptrauth-auth-traps" "ptrauth-calls" "ptrauth-indirect-gotos" "ptrauth-returns"

fun foo() = 42
