; OPT: --passes=kotlin-array-load-metadata

; Positive test: i32 load with _ZTS11ArrayHeader TBAA at count_ offset (8) gets !range and !invariant.load.
; CHECK-LABEL: define i32 @test_array_size_tbaa(
; CHECK-NEXT:    %2 = getelementptr inbounds i8, ptr %0, i64 8
; CHECK-NEXT:    %3 = load i32, ptr %2, align 8, !tbaa ![[TBAA_COUNT:[0-9]+]], !range ![[RANGE:[0-9]+]], !invariant.load ![[INV:[0-9]+]]{{$}}
; CHECK-NEXT:    ret i32 %3
define i32 @test_array_size_tbaa(ptr %0) {
  %2 = getelementptr inbounds i8, ptr %0, i64 8
  %3 = load i32, ptr %2, align 8, !tbaa !0
  ret i32 %3
}

; Negative test: load from _ZTS11ArrayHeader at offset 0 (typeInfoOrMeta_) must not be annotated.
; CHECK-LABEL: define i32 @test_array_header_offset_0(
; CHECK-NEXT:    %2 = load i32, ptr %0, align 8, !tbaa ![[TBAA_ZERO:[0-9]+]]{{$}}
; CHECK-NEXT:    ret i32 %2
define i32 @test_array_header_offset_0(ptr %0) {
  %2 = load i32, ptr %0, align 8, !tbaa !5
  ret i32 %2
}

; Negative test: i32 load at offset 8 inside a function with "Array" in its name but without
; _ZTS11ArrayHeader TBAA (e.g. a regular class field in ArrayList/fillArray) must not be annotated.
; CHECK-LABEL: define i32 @fillArray(
; CHECK-NEXT:    %2 = getelementptr inbounds i8, ptr %0, i64 8
; CHECK-NEXT:    %3 = load i32, ptr %2, align 8{{$}}
; CHECK-NEXT:    ret i32 %3
define i32 @fillArray(ptr %0) {
  %2 = getelementptr inbounds i8, ptr %0, i64 8
  %3 = load i32, ptr %2, align 8
  ret i32 %3
}

; Negative test: i32 load at offset 8 with a different struct TBAA (_ZTS12StringHeader) must not be annotated.
; CHECK-LABEL: define i32 @test_other_struct_tbaa(
; CHECK-NEXT:    %2 = getelementptr inbounds i8, ptr %0, i64 8
; CHECK-NEXT:    %3 = load i32, ptr %2, align 8, !tbaa ![[TBAA_OTHER:[0-9]+]]{{$}}
; CHECK-NEXT:    ret i32 %3
define i32 @test_other_struct_tbaa(ptr %0) {
  %2 = getelementptr inbounds i8, ptr %0, i64 8
  %3 = load i32, ptr %2, align 8, !tbaa !6
  ret i32 %3
}

; CHECK-DAG: ![[INV]] = !{}
; CHECK-DAG: ![[RANGE]] = !{i32 0, i32 -2147483648}

!0 = !{!1, !2, i64 8}
!1 = !{!"_ZTS11ArrayHeader", !2, i64 0, !2, i64 8}
!2 = !{!"int", !3, i64 0}
!3 = !{!"omnipotent char", !4, i64 0}
!4 = !{!"Simple C++ TBAA"}
!5 = !{!1, !2, i64 0}
!6 = !{!7, !2, i64 8}
!7 = !{!"_ZTS12StringHeader", !2, i64 0, !2, i64 8}
