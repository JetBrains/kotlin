; OPT: --passes=kotlin-ssp<req>

; CHECK: define void @f_defined() #0 {
define void @f_defined() {
  ret void
}

; CHECK: define void @__clang_call_terminate() {
define void @__clang_call_terminate() {
  ret void
}

; CHECK: declare void @f_declared(){{$}}
declare void @f_declared()

; CHECK: attributes #0 = { sspreq }
