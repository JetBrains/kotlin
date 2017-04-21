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

#include <stdlib.h>
#include <llvm-c/Analysis.h>
#include <DebugInfoC.h>
#include <common.h>
/**
 *  0:b-backend-dwarf:minamoto@unit-703(0)# clang -xc -S -g -emit-llvm -o - -
 *  "TERMCAP", line 20, col 1, terminal 'SC': Missing separator
 *    12345678901234567890
 *  1:int main() {
 *  2:  int a = 42;
 *  3:  return a;
 *  4:}
 *  ; ModuleID = '-'
 *  source_filename = "-"
 *  target datalayout = "e-m:o-i64:64-f80:128-n8:16:32:64-S128"
 *  target triple = "x86_64-apple-macosx10.12.0"
 *  
 *  ; Function Attrs: nounwind ssp uwtable
 *  define i32 @main() #0 !dbg !7 {
 *    %1 = alloca i32, align 4
 *    %2 = alloca i32, align 4
 *    store i32 0, i32* %1, align 4
 *    call void @llvm.dbg.declare(metadata i32* %2, metadata !12, metadata !13), !dbg !14
 *    store i32 42, i32* %2, align 4, !dbg !14
 *    %3 = load i32, i32* %2, align 4, !dbg !15
 *    ret i32 %3, !dbg !16
 *  }
 *  
 *  ; Function Attrs: nounwind readnone
 *  declare void @llvm.dbg.declare(metadata, metadata, metadata) #1
 *  
 *  attributes #0 = { nounwind ssp uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="penryn" "target-features"="+cx16,+fxsr,+mmx,+sse,+sse2,+sse3,+sse4.1,+ssse3,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
 *  attributes #1 = { nounwind readnone }
 *  
 *  !llvm.dbg.cu = !{!0}
 *  !llvm.module.flags = !{!3, !4, !5}
 *  !llvm.ident = !{!6}
 *  
 *  !0 = distinct !DICompileUnit(language: DW_LANG_C99, file: !1, producer: "Apple LLVM version 8.1.0 (clang-802.0.36)", isOptimized: false, runtimeVersion: 0, emissionKind: FullDebug, enums: !2)
 *  !1 = !DIFile(filename: "-", directory: "/Users/minamoto/ws/.git-trees/backend-dwarf")
 *  !2 = !{}
 *  !3 = !{i32 2, !"Dwarf Version", i32 4}
 *  !4 = !{i32 2, !"Debug Info Version", i32 700000003}
 *  !5 = !{i32 1, !"PIC Level", i32 2}
 *  !6 = !{!"Apple LLVM version 8.1.0 (clang-802.0.36)"}
 *  !7 = distinct !DISubprogram(name: "main", scope: !8, file: !8, line: 1, type: !9, isLocal: false, isDefinition: true, scopeLine: 1, isOptimized: false, unit: !0, variables: !2)
 *  !8 = !DIFile(filename: "<stdin>", directory: "/Users/minamoto/ws/.git-trees/backend-dwarf")
 *  !9 = !DISubroutineType(types: !10)
 *  !10 = !{!11}
 *  !11 = !DIBasicType(name: "int", size: 32, align: 32, encoding: DW_ATE_signed)
 *  !12 = !DILocalVariable(name: "a", scope: !7, file: !8, line: 2, type: !11)
 *  !13 = !DIExpression()
 *  !14 = !DILocation(line: 2, column: 7, scope: !7)
 *  !15 = !DILocation(line: 3, column: 10, scope: !7)
 *  !16 = !DILocation(line: 3, column: 3, scope: !7)
 */

static DIFileRef           file;
static DISubroutineTypeRef subroutine_type;
static LLVMTypeRef         int_type;
static DITypeOpaqueRef     di_int_type;


static void
create_main() {
  DISubprogramRef di;
  location loc = {&file, 1};
  create_function_with_entry("main", loc, subroutine_type, &di);
  LLVMValueRef address = LLVMBuildAlloca(g_codegen.llvm_builder, int_type, "");
  DILocationRef location = LLVMBuilderSetDebugLocation(g_codegen.llvm_builder,
                                                       2, 1, SCOPE(di));
  DILocalVariableRef variable_a = DICreateAutoVariable(g_codegen.di_builder, SCOPE(di),
                                                       "a", file, 2, di_int_type);
  DIInsertDeclarationWithEmptyExpression(g_codegen.di_builder, address, variable_a,
                                         location, LLVMGetInsertBlock(g_codegen.llvm_builder));
  LLVMBuildStore(g_codegen.llvm_builder, LLVMConstInt(int_type, 42, 1), address);
  LLVMValueRef value = LLVMBuildLoad(g_codegen.llvm_builder, address, "");
  LLVMBuildRet(g_codegen.llvm_builder, value);
}


int
main() {
  codegen_init();
  file             = DICreateFile(g_codegen.di_builder, "<stdin>", "");
  subroutine_type  = DICreateSubroutineType(g_codegen.di_builder, NULL, 0);
  int_type         = LLVMInt32Type();
  di_int_type      = TYPE(DICreateBasicType(g_codegen.di_builder, "int", 32, 4, 0));
  create_main();
  codegen_destroy();
}
