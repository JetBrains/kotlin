; OPT: --passes=function(kotlin-array-load-metadata),default<O3>
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

declare void @throwIndexOutOfBoundsException() noreturn
declare void @throwIllegalArgumentException() noreturn

; Verify that under default<O3>, annotating ArrayHeader::count_ with !invariant.load and !range
; allows LLVM to:
; 1. Hoist and CSE repeated ArrayHeader::count_ loads across ByteArray element stores.
; 2. Prove that bounds checks (offset + 0 < size, offset + 1 < size, offset + 2 < size, offset + 3 < size)
;    are redundant given require(0 <= offset && offset <= size - 4).
;
; CHECK-LABEL: define void @writeIntLe(
; CHECK:       load i32, ptr %{{.*}}, align 8, !tbaa ![[TBAA:[0-9]+]], !range ![[RANGE:[0-9]+]], !invariant.load ![[INV:[0-9]+]]
; CHECK-NOT:   load i32
; CHECK-NOT:   call void @throwIndexOutOfBoundsException()
; CHECK:       ret void
define void @writeIntLe(ptr %data, i32 %offset, i32 %val) {
entry:
  %offset.nonneg = icmp sge i32 %offset, 0
  br i1 %offset.nonneg, label %check.require, label %fail.require

check.require:
  %size.ptr = getelementptr inbounds i8, ptr %data, i64 8
  %size.0 = load i32, ptr %size.ptr, align 8, !tbaa !0
  %sub = add nsw i32 %size.0, -4
  %require.ok = icmp sle i32 %offset, %sub
  br i1 %require.ok, label %body, label %fail.require

fail.require:
  call void @throwIllegalArgumentException()
  unreachable

body:
  %b0 = trunc i32 %val to i8
  %s1 = lshr i32 %val, 8
  %b1 = trunc i32 %s1 to i8
  %s2 = lshr i32 %val, 16
  %b2 = trunc i32 %s2 to i8
  %s3 = lshr i32 %val, 24
  %b3 = trunc i32 %s3 to i8

  %size.1 = load i32, ptr %size.ptr, align 8, !tbaa !0
  %bc0 = icmp ult i32 %offset, %size.1
  br i1 %bc0, label %store0, label %oob

store0:
  %base = getelementptr inbounds i8, ptr %data, i64 16
  %idx0 = zext nneg i32 %offset to i64
  %p0 = getelementptr inbounds i8, ptr %base, i64 %idx0
  store i8 %b0, ptr %p0, align 1

  %i1 = add nuw nsw i32 %offset, 1
  %size.2 = load i32, ptr %size.ptr, align 8, !tbaa !0
  %bc1 = icmp ult i32 %i1, %size.2
  br i1 %bc1, label %store1, label %oob

store1:
  %idx1 = zext nneg i32 %i1 to i64
  %p1 = getelementptr inbounds i8, ptr %base, i64 %idx1
  store i8 %b1, ptr %p1, align 1

  %i2 = add nuw nsw i32 %offset, 2
  %size.3 = load i32, ptr %size.ptr, align 8, !tbaa !0
  %bc2 = icmp ult i32 %i2, %size.3
  br i1 %bc2, label %store2, label %oob

store2:
  %idx2 = zext nneg i32 %i2 to i64
  %p2 = getelementptr inbounds i8, ptr %base, i64 %idx2
  store i8 %b2, ptr %p2, align 1

  %i3 = add nuw nsw i32 %offset, 3
  %size.4 = load i32, ptr %size.ptr, align 8, !tbaa !0
  %bc3 = icmp ult i32 %i3, %size.4
  br i1 %bc3, label %store3, label %oob

store3:
  %idx3 = zext nneg i32 %i3 to i64
  %p3 = getelementptr inbounds i8, ptr %base, i64 %idx3
  store i8 %b3, ptr %p3, align 1
  ret void

oob:
  call void @throwIndexOutOfBoundsException()
  unreachable
}

!0 = !{!1, !2, i64 8}
!1 = !{!"_ZTS11ArrayHeader", !2, i64 0, !2, i64 8}
!2 = !{!"int", !3, i64 0}
!3 = !{!"omnipotent char", !4, i64 0}
!4 = !{!"Simple C++ TBAA"}
