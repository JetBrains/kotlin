; OPT: --passes=kotlin-tsan

; CHECK: define void @f_defined() #0 {
define void @f_defined() {
  ret void
}

; CHECK: declare void @f_declared(){{$}}
declare void @f_declared()

; CHECK: attributes #0 = { sanitize_thread }
