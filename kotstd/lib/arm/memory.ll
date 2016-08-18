; ModuleID = '/home/user/Documents/carkot/kotstd/libc/memory.c'
target datalayout = "e-m:e-p:32:32-i64:64-v128:64:128-a:0:32-n32-S64"
target triple = "thumbv7m-none--eabi"

@static_area = common global [30000 x i8] zeroinitializer, align 1
@dynamic_area = common global [30000 x i8] zeroinitializer, align 1
@heaps = global [2 x i8*] [i8* getelementptr inbounds ([30000 x i8]* @static_area, i32 0, i32 0), i8* getelementptr inbounds ([30000 x i8]* @dynamic_area, i32 0, i32 0)], align 4
@heap_tails = global [2 x i32] zeroinitializer, align 4
@active_heap = global i32 0, align 4
@dynamic_heap_consume = global i32 0, align 4
@dynamic_heap_max = global i32 0, align 4

; Function Attrs: nounwind
define i8* @malloc_heap(i32 %size) #0 {
  %1 = alloca i32, align 4
  %ptr = alloca i8*, align 4
  store i32 %size, i32* %1, align 4
  call void @llvm.dbg.declare(metadata i32* %1, metadata !37, metadata !38), !dbg !39
  call void @llvm.dbg.declare(metadata i8** %ptr, metadata !40, metadata !38), !dbg !41
  %2 = load i32* @active_heap, align 4, !dbg !42
  %3 = getelementptr inbounds [2 x i8*]* @heaps, i32 0, i32 %2, !dbg !43
  %4 = load i8** %3, align 4, !dbg !43
  %5 = load i32* @active_heap, align 4, !dbg !44
  %6 = getelementptr inbounds [2 x i32]* @heap_tails, i32 0, i32 %5, !dbg !45
  %7 = load i32* %6, align 4, !dbg !45
  %8 = getelementptr inbounds i8* %4, i32 %7, !dbg !43
  store i8* %8, i8** %ptr, align 4, !dbg !41
  %9 = load i32* %1, align 4, !dbg !46
  %10 = load i32* @active_heap, align 4, !dbg !47
  %11 = getelementptr inbounds [2 x i32]* @heap_tails, i32 0, i32 %10, !dbg !48
  %12 = load i32* %11, align 4, !dbg !48
  %13 = add nsw i32 %12, %9, !dbg !48
  store i32 %13, i32* %11, align 4, !dbg !48
  %14 = load i8** %ptr, align 4, !dbg !49
  ret i8* %14, !dbg !50
}

; Function Attrs: nounwind readnone
declare void @llvm.dbg.declare(metadata, metadata, metadata) #1

; Function Attrs: nounwind
define void @set_active_heap(i32 %heap) #0 {
  %1 = alloca i32, align 4
  store i32 %heap, i32* %1, align 4
  call void @llvm.dbg.declare(metadata i32* %1, metadata !51, metadata !38), !dbg !52
  %2 = load i32* %1, align 4, !dbg !53
  store i32 %2, i32* @active_heap, align 4, !dbg !54
  ret void, !dbg !55
}

; Function Attrs: nounwind
define void @clean_dynamic_heap() #0 {
  %1 = load i32* getelementptr inbounds ([2 x i32]* @heap_tails, i32 0, i32 1), align 4, !dbg !56
  %2 = load i32* @dynamic_heap_consume, align 4, !dbg !57
  %3 = add nsw i32 %2, %1, !dbg !57
  store i32 %3, i32* @dynamic_heap_consume, align 4, !dbg !57
  %4 = load i32* getelementptr inbounds ([2 x i32]* @heap_tails, i32 0, i32 1), align 4, !dbg !58
  %5 = load i32* @dynamic_heap_max, align 4, !dbg !60
  %6 = icmp sgt i32 %4, %5, !dbg !58
  br i1 %6, label %7, label %9, !dbg !61

; <label>:7                                       ; preds = %0
  %8 = load i32* getelementptr inbounds ([2 x i32]* @heap_tails, i32 0, i32 1), align 4, !dbg !62
  store i32 %8, i32* @dynamic_heap_max, align 4, !dbg !64
  br label %9, !dbg !65

; <label>:9                                       ; preds = %7, %0
  store i32 0, i32* getelementptr inbounds ([2 x i32]* @heap_tails, i32 0, i32 1), align 4, !dbg !66
  ret void, !dbg !67
}

attributes #0 = { nounwind "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "stack-protector-buffer-size"="8" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { nounwind readnone }

!llvm.dbg.cu = !{!0}
!llvm.module.flags = !{!32, !33, !34, !35}
!llvm.ident = !{!36}

!0 = !{!"0x11\0012\00Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)\000\00\000\00\001", !1, !2, !2, !3, !17, !2} ; [ DW_TAG_compile_unit ] [/home/user/Documents/carkot/kotstd//home/user/Documents/carkot/kotstd/libc/memory.c] [DW_LANG_C99]
!1 = !{!"/home/user/Documents/carkot/kotstd/libc/memory.c", !"/home/user/Documents/carkot/kotstd"}
!2 = !{}
!3 = !{!4, !11, !14}
!4 = !{!"0x2e\00malloc_heap\00malloc_heap\00\0028\000\001\000\000\00256\000\0028", !1, !5, !6, null, i8* (i32)* @malloc_heap, null, null, !2} ; [ DW_TAG_subprogram ] [line 28] [def] [malloc_heap]
!5 = !{!"0x29", !1}                               ; [ DW_TAG_file_type ] [/home/user/Documents/carkot/kotstd//home/user/Documents/carkot/kotstd/libc/memory.c]
!6 = !{!"0x15\00\000\000\000\000\000\000", null, null, null, !7, null, null, null} ; [ DW_TAG_subroutine_type ] [line 0, size 0, align 0, offset 0] [from ]
!7 = !{!8, !10}
!8 = !{!"0xf\00\000\0032\0032\000\000", null, null, !9} ; [ DW_TAG_pointer_type ] [line 0, size 32, align 32, offset 0] [from char]
!9 = !{!"0x24\00char\000\008\008\000\000\008", null, null} ; [ DW_TAG_base_type ] [char] [line 0, size 8, align 8, offset 0, enc DW_ATE_unsigned_char]
!10 = !{!"0x24\00int\000\0032\0032\000\000\005", null, null} ; [ DW_TAG_base_type ] [int] [line 0, size 32, align 32, offset 0, enc DW_ATE_signed]
!11 = !{!"0x2e\00set_active_heap\00set_active_heap\00\0040\000\001\000\000\00256\000\0040", !1, !5, !12, null, void (i32)* @set_active_heap, null, null, !2} ; [ DW_TAG_subprogram ] [line 40] [def] [set_active_heap]
!12 = !{!"0x15\00\000\000\000\000\000\000", null, null, null, !13, null, null, null} ; [ DW_TAG_subroutine_type ] [line 0, size 0, align 0, offset 0] [from ]
!13 = !{null, !10}
!14 = !{!"0x2e\00clean_dynamic_heap\00clean_dynamic_heap\00\0046\000\001\000\000\000\000\0046", !1, !5, !15, null, void ()* @clean_dynamic_heap, null, null, !2} ; [ DW_TAG_subprogram ] [line 46] [def] [clean_dynamic_heap]
!15 = !{!"0x15\00\000\000\000\000\000\000", null, null, null, !16, null, null, null} ; [ DW_TAG_subroutine_type ] [line 0, size 0, align 0, offset 0] [from ]
!16 = !{null}
!17 = !{!18, !22, !24, !25, !26, !27, !31}
!18 = !{!"0x34\00heaps\00heaps\00\0013\000\001", null, !5, !19, [2 x i8*]* @heaps, null} ; [ DW_TAG_variable ] [heaps] [line 13] [def]
!19 = !{!"0x1\00\000\0064\0032\000\000\000", null, null, !8, !20, null, null, null} ; [ DW_TAG_array_type ] [line 0, size 64, align 32, offset 0] [from ]
!20 = !{!21}
!21 = !{!"0x21\000\002"}                          ; [ DW_TAG_subrange_type ] [0, 1]
!22 = !{!"0x34\00heap_tails\00heap_tails\00\0018\000\001", null, !5, !23, [2 x i32]* @heap_tails, null} ; [ DW_TAG_variable ] [heap_tails] [line 18] [def]
!23 = !{!"0x1\00\000\0064\0032\000\000\000", null, null, !10, !20, null, null, null} ; [ DW_TAG_array_type ] [line 0, size 64, align 32, offset 0] [from int]
!24 = !{!"0x34\00active_heap\00active_heap\00\0019\000\001", null, !5, !10, i32* @active_heap, null} ; [ DW_TAG_variable ] [active_heap] [line 19] [def]
!25 = !{!"0x34\00dynamic_heap_consume\00dynamic_heap_consume\00\0021\000\001", null, !5, !10, i32* @dynamic_heap_consume, null} ; [ DW_TAG_variable ] [dynamic_heap_consume] [line 21] [def]
!26 = !{!"0x34\00dynamic_heap_max\00dynamic_heap_max\00\0022\000\001", null, !5, !10, i32* @dynamic_heap_max, null} ; [ DW_TAG_variable ] [dynamic_heap_max] [line 22] [def]
!27 = !{!"0x34\00static_area\00static_area\00\0010\000\001", null, !5, !28, [30000 x i8]* @static_area, null} ; [ DW_TAG_variable ] [static_area] [line 10] [def]
!28 = !{!"0x1\00\000\00240000\008\000\000\000", null, null, !9, !29, null, null, null} ; [ DW_TAG_array_type ] [line 0, size 240000, align 8, offset 0] [from char]
!29 = !{!30}
!30 = !{!"0x21\000\0030000"}                      ; [ DW_TAG_subrange_type ] [0, 29999]
!31 = !{!"0x34\00dynamic_area\00dynamic_area\00\0011\000\001", null, !5, !28, [30000 x i8]* @dynamic_area, null} ; [ DW_TAG_variable ] [dynamic_area] [line 11] [def]
!32 = !{i32 2, !"Dwarf Version", i32 4}
!33 = !{i32 2, !"Debug Info Version", i32 2}
!34 = !{i32 1, !"wchar_size", i32 4}
!35 = !{i32 1, !"min_enum_size", i32 4}
!36 = !{!"Ubuntu clang version 3.6.2-3ubuntu2 (tags/RELEASE_362/final) (based on LLVM 3.6.2)"}
!37 = !{!"0x101\00size\0016777244\000", !4, !5, !10} ; [ DW_TAG_arg_variable ] [size] [line 28]
!38 = !{!"0x102"}                                 ; [ DW_TAG_expression ]
!39 = !MDLocation(line: 28, column: 23, scope: !4)
!40 = !{!"0x100\00ptr\0030\000", !4, !5, !8}      ; [ DW_TAG_auto_variable ] [ptr] [line 30]
!41 = !MDLocation(line: 30, column: 11, scope: !4)
!42 = !MDLocation(line: 30, column: 23, scope: !4)
!43 = !MDLocation(line: 30, column: 17, scope: !4)
!44 = !MDLocation(line: 30, column: 49, scope: !4)
!45 = !MDLocation(line: 30, column: 38, scope: !4)
!46 = !MDLocation(line: 31, column: 32, scope: !4)
!47 = !MDLocation(line: 31, column: 16, scope: !4)
!48 = !MDLocation(line: 31, column: 5, scope: !4)
!49 = !MDLocation(line: 33, column: 12, scope: !4)
!50 = !MDLocation(line: 33, column: 5, scope: !4)
!51 = !{!"0x101\00heap\0016777256\000", !11, !5, !10} ; [ DW_TAG_arg_variable ] [heap] [line 40]
!52 = !MDLocation(line: 40, column: 26, scope: !11)
!53 = !MDLocation(line: 42, column: 19, scope: !11)
!54 = !MDLocation(line: 42, column: 5, scope: !11)
!55 = !MDLocation(line: 44, column: 1, scope: !11)
!56 = !MDLocation(line: 48, column: 29, scope: !14)
!57 = !MDLocation(line: 48, column: 5, scope: !14)
!58 = !MDLocation(line: 49, column: 9, scope: !59)
!59 = !{!"0xb\0049\009\000", !1, !14}             ; [ DW_TAG_lexical_block ] [/home/user/Documents/carkot/kotstd//home/user/Documents/carkot/kotstd/libc/memory.c]
!60 = !MDLocation(line: 49, column: 36, scope: !59)
!61 = !MDLocation(line: 49, column: 9, scope: !14)
!62 = !MDLocation(line: 50, column: 28, scope: !63)
!63 = !{!"0xb\0049\0054\001", !1, !59}            ; [ DW_TAG_lexical_block ] [/home/user/Documents/carkot/kotstd//home/user/Documents/carkot/kotstd/libc/memory.c]
!64 = !MDLocation(line: 50, column: 9, scope: !63)
!65 = !MDLocation(line: 51, column: 5, scope: !63)
!66 = !MDLocation(line: 53, column: 5, scope: !14)
!67 = !MDLocation(line: 55, column: 1, scope: !14)
