/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <DebugInfoC.h>
#include <common.h>
 /**
  * 0:b-debugger-types:minamoto@unit-703(0)# clang -g -xc -emit-llvm -S -o - -
  * struct A{
  * int a;
  * int b;
  * };
  *
  * struct A* foo(struct A* a) {
  *  a->a += 1;
  *  return a;
  * }
  * ; ModuleID = '-'
  * source_filename = "-"
  * target datalayout = "e-m:o-i64:64-f80:128-n8:16:32:64-S128"
  * target triple = "x86_64-apple-macosx10.12.0"
  *
  * %struct.A = type { i32, i32 }
  *
  * ; Function Attrs: nounwind ssp uwtable
  * define %struct.A* @foo(%struct.A*) #0 !dbg !7 {
  *   %2 = alloca %struct.A*, align 8
  *   store %struct.A* %0, %struct.A** %2, align 8
  *   call void @llvm.dbg.declare(metadata %struct.A** %2, metadata !17, metadata !18), !dbg !19
  *   %3 = load %struct.A*, %struct.A** %2, align 8, !dbg !20
  *   %4 = getelementptr inbounds %struct.A, %struct.A* %3, i32 0, i32 0, !dbg !21
  *   %5 = load i32, i32* %4, align 4, !dbg !22
  *   %6 = add nsw i32 %5, 1, !dbg !22
  *   store i32 %6, i32* %4, align 4, !dbg !22
  *   %7 = load %struct.A*, %struct.A** %2, align 8, !dbg !23
  *   ret %struct.A* %7, !dbg !24
  * }
  *
  * ; Function Attrs: nounwind readnone
  * declare void @llvm.dbg.declare(metadata, metadata, metadata) #1
  *
  * attributes #0 = { nounwind ssp uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="penryn" "target-features"="+cx16,+fxsr,+mmx,+sse,+sse2,+sse3,+sse4.1,+ssse3,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
  * attributes #1 = { nounwind readnone }
  *
  * !llvm.dbg.cu = !{!0}
  * !llvm.module.flags = !{!3, !4, !5}
  * !llvm.ident = !{!6}
  *
  * !0 = distinct !DICompileUnit(language: DW_LANG_C99, file: !1, producer: "Apple LLVM version 8.1.0 (clang-802.0.36)", isOptimized: false, runtimeVersion: 0, emissionKind: FullDebug, enums: !2)
  * !1 = !DIFile(filename: "-", directory: "/Users/minamoto/ws/.git-trees/debugger-types")
  * !2 = !{}
  * !3 = !{i32 2, !"Dwarf Version", i32 4}
  * !4 = !{i32 2, !"Debug Info Version", i32 700000003}
  * !5 = !{i32 1, !"PIC Level", i32 2}
  * !6 = !{!"Apple LLVM version 8.1.0 (clang-802.0.36)"}
  * !7 = distinct !DISubprogram(name: "foo", scope: !8, file: !8, line: 6, type: !9, isLocal: false, isDefinition: true, scopeLine: 6, flags: DIFlagPrototyped, isOptimized: false, unit: !0, variables: !2)
  * !8 = !DIFile(filename: "<stdin>", directory: "/Users/minamoto/ws/.git-trees/debugger-types")
  * !9 = !DISubroutineType(types: !10)
  * !10 = !{!11, !11}
  * !11 = !DIDerivedType(tag: DW_TAG_pointer_type, baseType: !12, size: 64, align: 64)
  * !12 = distinct !DICompositeType(tag: DW_TAG_structure_type, name: "A", file: !8, line: 1, size: 64, align: 32, elements: !13)
  * !13 = !{!14, !16}
  * !14 = !DIDerivedType(tag: DW_TAG_member, name: "a", scope: !12, file: !8, line: 2, baseType: !15, size: 32, align: 32)
  * !15 = !DIBasicType(name: "int", size: 32, align: 32, encoding: DW_ATE_signed)
  * !16 = !DIDerivedType(tag: DW_TAG_member, name: "b", scope: !12, file: !8, line: 3, baseType: !15, size: 32, align: 32, offset: 32)
  * !17 = !DILocalVariable(name: "a", arg: 1, scope: !7, file: !8, line: 6, type: !11)
  * !18 = !DIExpression()
  * !19 = !DILocation(line: 6, column: 25, scope: !7)
  * !20 = !DILocation(line: 7, column: 2, scope: !7)
  * !21 = !DILocation(line: 7, column: 5, scope: !7)
  * !22 = !DILocation(line: 7, column: 7, scope: !7)
  * !23 = !DILocation(line: 8, column: 9, scope: !7)
  * !24 = !DILocation(line: 8, column: 2, scope: !7)
  */
static DIFileRef           file;
static DISubroutineTypeRef subroutine_type;
static LLVMTypeRef         int_type;

static void
create_main() {
  DISubprogramRef di;
  location loc = {&file, 1};
  create_function_with_entry("main", loc, subroutine_type, &di);
  LLVMBuildRet(g_codegen.llvm_builder, LLVMConstInt(LLVMInt32Type(), 42, 1));
}


int
main() {
  codegen_init();
  DITypeOpaqueRef int_type  = TYPE(DICreateBasicType(g_codegen.di_builder, "int", 32, 4, 0));
  DIDerivedTypeRef elements_int_type[2];
  file             = DICreateFile(g_codegen.di_builder, "<stdin>", "");
  DICompositeTypeRef tempA = DICreateReplaceableCompositeType(g_codegen.di_builder, "A", SCOPE(g_codegen.di_compile_unit), file, 2);
  elements_int_type[0] = DICreateMemberType(
                                /* builder      = */ g_codegen.di_builder,
                                /* scope        = */ SCOPE(tempA), /* note: here dump points to structure ???*/
                                /* name         = */ "a",
                                /* file         = */ file,
                                /* lineNum      = */ 3,
                                /* sizeInBits   = */ 32,
                                /* alignInBits  = */ 32,
                                /* offsetInBits = */ 0,
                                /* flags        = */ 0,
                                /* type         = */ int_type);
  elements_int_type[1] = DICreateMemberType(
                                /* builder      = */ g_codegen.di_builder,
                                /* scope        = */ SCOPE(tempA),
                                /* name         = */ "b",
                                /* file         = */ file,
                                /* lineNum      = */ 4,
                                /* sizeInBits   = */ 32,
                                /* alignInBits  = */ 32,
                                /* offsetInBits = */ 32,
                                /* flags        = */ 0,
                                /* type         = */ int_type);

  DITypeOpaqueRef A_type  = TYPE(DICreateStructType(
                                   g_codegen.di_builder,
                                   g_codegen.di_compile_unit,
                                   "A", file, 2, 64, 32, 0, (void *)0, elements_int_type, 2, tempA));
   subroutine_type  = DICreateSubroutineType(g_codegen.di_builder, &A_type, 1);
   create_main();
   codegen_destroy();
}
