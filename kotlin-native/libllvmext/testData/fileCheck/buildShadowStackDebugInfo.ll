; OPT: --passes=kotlin-build-shadow-stack,verify
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

attributes #0 = { "kotlin-gc-frame" }

declare void @safepoint_1()
declare void @use_func(ptr addrspace(1))
declare ptr addrspace(1) @produce()

define void @EnterFrame(ptr %f, i32 %a, i32 %b) !dbg !9 {
  %count = getelementptr inbounds i8, ptr %f, i64 12
  store i32 %b, ptr %count, align 4, !dbg !11
  ret void, !dbg !11
}

define void @LeaveFrame(ptr %f, i32 %a, i32 %b) !dbg !10 {
  store ptr null, ptr %f, align 8, !dbg !12
  ret void, !dbg !12
}

; CHECK-LABEL: define void @prologue_without_location()
; CHECK-SAME: !dbg ![[CALLER_SP:[0-9]+]] {
; CHECK: call void @llvm.memset{{.*}}, !dbg !
; CHECK-NOT: call void @EnterFrame
; CHECK: store i32 3, ptr %count.i, align 4, !dbg ![[ENTER_LOC:[0-9]+]]
; CHECK-NOT: call void @LeaveFrame
; CHECK: store ptr null, ptr %shadow_stack_frame, align 8, !dbg ![[LEAVE_LOC:[0-9]+]]
; CHECK: ret void
define void @prologue_without_location() #0 !dbg !4 {
prologue:
  br label %entry

entry:
  %obj = call ptr addrspace(1) @produce(), !dbg !8
  call void @safepoint_1(), !dbg !8
  call void @use_func(ptr addrspace(1) %obj), !dbg !8
  ret void, !dbg !8
}

; CHECK-LABEL: define void @no_debug_info()
; CHECK-NOT: !dbg
; CHECK: ret void
; CHECK-NEXT: }

; CHECK: ![[ENTER_SP:[0-9]+]] = distinct !DISubprogram(name: "EnterFrame"
; CHECK: ![[LEAVE_SP:[0-9]+]] = distinct !DISubprogram(name: "LeaveFrame"
; CHECK: ![[ENTER_LOC]] = !DILocation(line: 11, scope: ![[ENTER_SP]], inlinedAt: ![[ENTER_CALL:[0-9]+]])
; CHECK-NEXT: ![[ENTER_CALL]] = distinct !DILocation(line: 0, scope: ![[CALLER_SP]])
; CHECK: ![[LEAVE_LOC]] = !DILocation(line: 21, scope: ![[LEAVE_SP]], inlinedAt: ![[LEAVE_CALL:[0-9]+]])
; CHECK-NEXT: ![[LEAVE_CALL]] = distinct !DILocation(line: 2, column: 1, scope: ![[CALLER_SP]])
define void @no_debug_info() #0 {
entry:
  %obj = call ptr addrspace(1) @produce()
  call void @safepoint_1()
  call void @use_func(ptr addrspace(1) %obj)
  ret void
}

!llvm.dbg.cu = !{!0}
!llvm.module.flags = !{!2, !3}

!0 = distinct !DICompileUnit(language: DW_LANG_C99, file: !1, producer: "test", isOptimized: false, runtimeVersion: 0, emissionKind: FullDebug)
!1 = !DIFile(filename: "debugInfo.kt", directory: "/tmp")
!2 = !{i32 2, !"Debug Info Version", i32 3}
!3 = !{i32 7, !"Dwarf Version", i32 4}
!4 = distinct !DISubprogram(name: "prologue_without_location", scope: !1, file: !1, line: 1, type: !5, scopeLine: 1, spFlags: DISPFlagDefinition, unit: !0)
!5 = !DISubroutineType(types: !6)
!6 = !{null, !7}
!7 = !DIBasicType(name: "ptr", size: 64, encoding: DW_ATE_address)
!8 = !DILocation(line: 2, column: 1, scope: !4)
!9 = distinct !DISubprogram(name: "EnterFrame", scope: !1, file: !1, line: 10, type: !5, scopeLine: 10, spFlags: DISPFlagDefinition, unit: !0)
!10 = distinct !DISubprogram(name: "LeaveFrame", scope: !1, file: !1, line: 20, type: !5, scopeLine: 20, spFlags: DISPFlagDefinition, unit: !0)
!11 = !DILocation(line: 11, scope: !9)
!12 = !DILocation(line: 21, scope: !10)
