; OPT: --passes=kotlin-hide-symbols

@llvm.used = appending global [3 x ptr] [ptr @g_used, ptr @a_used, ptr @f_defined_used]

; CHECK: @g_public = hidden global
@g_public = global i32 0

; CHECK: @g_private = private global
@g_private = private global i32 0

; CHECK: @g_internal = internal global
@g_internal = internal global i32 0

; CHECK: @g_used = global
@g_used = global i32 0

; CHECK: @a_public = hidden alias
@a_public = alias i32, ptr @g_public

; CHECK: @a_private = private alias
@a_private = private alias i32, ptr @g_public

; CHECK: @a_internal = internal alias
@a_internal = internal alias i32, ptr @g_public

; CHECK: @a_used = alias
@a_used = alias i32, ptr @g_public

; CHECK: define hidden void @f_defined() {
define void @f_defined() {
  ret void
}

; CHECK: define private void @f_defined_private() {
define private void @f_defined_private() {
  ret void
}

; CHECK: define internal void @f_defined_internal() {
define internal void @f_defined_internal() {
  ret void
}

; CHECK: define void @f_defined_used() {
define void @f_defined_used() {
  ret void
}

; CHECK: declare void @f_declared()
declare void @f_declared()
