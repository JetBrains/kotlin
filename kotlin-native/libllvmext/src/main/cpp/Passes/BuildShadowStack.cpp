// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "BuildShadowStack.h"

#include "llvm-c/Core.h"
#include "llvm/ADT/BitVector.h"
#include "llvm/ADT/DenseMap.h"
#include "llvm/ADT/DenseSet.h"
#include "llvm/ADT/PostOrderIterator.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/ADT/SetVector.h"
#include "llvm/ADT/SmallPtrSet.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/Analysis/CFG.h"
#include "llvm/Analysis/ValueTracking.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/DataLayout.h"
#include "llvm/IR/DebugInfo.h"
#include "llvm/IR/DebugInfoMetadata.h"
#include "llvm/IR/DebugLoc.h"
#include "llvm/IR/Dominators.h"
#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/GlobalValue.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/InstIterator.h"
#include "llvm/IR/InstrTypes.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/IntrinsicInst.h"
#include "llvm/IR/Intrinsics.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Operator.h"
#include "llvm/IR/ValueHandle.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/Utils/BasicBlockUtils.h"
#include "llvm/Transforms/Utils/Cloning.h"
#include "llvm/Transforms/Utils/PromoteMemToReg.h"

#include <algorithm>
#include <optional>
#include <string>
#include <type_traits>
#include <vector>

using namespace llvm;
using namespace llvm::kotlin;

static constexpr unsigned GCAddressSpace = BuildShadowStackPass::GCAddressSpace;

static constexpr const char *ReturnSlotMarkerName = "Kotlin_gc_returnSlot";

static constexpr const char *FrameEnterMarkerName = "Kotlin_gc_frameEnter";
static constexpr const char *FrameLeaveMarkerName = "Kotlin_gc_frameLeave";

static constexpr const char *FrameSetCurrentMarkerName =
    "Kotlin_gc_frameSetCurrent";

static constexpr const char *KotlinLandingPadMetadataName =
    "kotlin.gc.landingpad";

static constexpr const char *StackObjectMarkerName = "Kotlin_gc_stackObject";

static constexpr const char *KeepAliveMarkerName = "Kotlin_gc_keepAlive";

static constexpr const char *KotlinGcFrameAttrName = "kotlin-gc-frame";

struct ValueTypeAccess : GlobalValue {
  static constexpr Type *GlobalValue::*Field = &ValueTypeAccess::ValueType;
};

static void setFunctionType(Function &F, FunctionType *FTy) {
  F.*ValueTypeAccess::Field = FTy;
}

static unsigned getFrameOverlaySlots(const Module &M) {
  const DataLayout &DL = M.getDataLayout();
  LLVMContext &Ctx = M.getContext();

  if (auto *ST = StructType::getTypeByName(Ctx, "struct.FrameOverlay")) {
    if (!ST->isOpaque() && DL.getPointerSize() > 0)
      return static_cast<unsigned>(DL.getTypeStoreSize(ST) / DL.getPointerSize());
  }
  if (auto *ST = StructType::getTypeByName(Ctx, "FrameOverlay")) {
    if (!ST->isOpaque() && DL.getPointerSize() > 0)
      return static_cast<unsigned>(DL.getTypeStoreSize(ST) / DL.getPointerSize());
  }

  Type *PtrTy = PointerType::getUnqual(Ctx);
  Type *I32Ty = Type::getInt32Ty(Ctx);
  StructType *FO = StructType::get(Ctx, {PtrTy, I32Ty, I32Ty});
  uint64_t structSize = DL.getTypeStoreSize(FO);
  uint64_t ptrSize = DL.getPointerSize();
  if (ptrSize > 0 && structSize % ptrSize == 0)
    return static_cast<unsigned>(structSize / ptrSize);

  return 2;
}

static FunctionCallee getOrCreateEnterFrame(Module &M) {
  LLVMContext &Ctx = M.getContext();
  FunctionType *FTy = FunctionType::get(
      Type::getVoidTy(Ctx),
      {PointerType::getUnqual(Ctx), Type::getInt32Ty(Ctx), Type::getInt32Ty(Ctx)},
      false);
  return M.getOrInsertFunction("EnterFrame", FTy);
}

static FunctionCallee getOrCreateLeaveFrame(Module &M) {
  LLVMContext &Ctx = M.getContext();
  FunctionType *FTy = FunctionType::get(
      Type::getVoidTy(Ctx),
      {PointerType::getUnqual(Ctx), Type::getInt32Ty(Ctx), Type::getInt32Ty(Ctx)},
      false);
  return M.getOrInsertFunction("LeaveFrame", FTy);
}

static FunctionCallee getOrCreateSetCurrentFrame(Module &M) {
  LLVMContext &Ctx = M.getContext();
  FunctionType *FTy = FunctionType::get(
      Type::getVoidTy(Ctx),
      {PointerType::getUnqual(Ctx)},
      false);
  return M.getOrInsertFunction("SetCurrentFrame", FTy);
}

static bool isInteriorPointerFunction(const Function &F) {
  StringRef Name = F.getName();
  return Name == "Kotlin_arrayGetElementAddress" ||
         Name == "Kotlin_byteArrayGetElementAddress" ||
         Name == "Kotlin_shortArrayGetElementAddress" ||
         Name == "Kotlin_charArrayGetElementAddress" ||
         Name == "Kotlin_booleanArrayGetElementAddress" ||
         Name == "Kotlin_intArrayGetElementAddress" ||
         Name == "Kotlin_longArrayGetElementAddress" ||
         Name == "Kotlin_floatArrayGetElementAddress" ||
         Name == "Kotlin_doubleArrayGetElementAddress";
}

static bool isNonSafepointFunction(const Function &F) {
  if (F.isIntrinsic())
    return true;

  StringRef Name = F.getName();
  if (Name == "EnterFrame" || Name == "LeaveFrame" ||
      Name == "SetCurrentFrame" || Name == "CheckCurrentFrame")
    return true;

  if (isInteriorPointerFunction(F))
    return true;

  if (Name == "Kotlin_Any_getTypeInfo" ||
      Name == "__cxa_begin_catch" || Name == "__cxa_end_catch")
    return true;

  if (Name.starts_with("Kotlin_mm_switchThreadState"))
    return true;

  if (Name == ReturnSlotMarkerName || Name == FrameEnterMarkerName ||
      Name == FrameLeaveMarkerName || Name == FrameSetCurrentMarkerName ||
      Name == StackObjectMarkerName || Name == KeepAliveMarkerName)
    return true;

  return false;
}

static void collectMarkerCalls(Function &F, const char *Name,
                               SmallVectorImpl<CallInst *> &Out) {
  Function *Marker = F.getParent()->getFunction(Name);
  if (!Marker)
    return;
  for (User *U : Marker->users()) {
    auto *CI = dyn_cast<CallInst>(U);
    if (CI && CI->getFunction() == &F)
      Out.push_back(CI);
  }
}

static bool isSafepointOrAllocationCall(const Instruction &I) {
  auto *Call = dyn_cast<CallBase>(&I);
  if (!Call)
    return false;

  if (Call->isInlineAsm())
    return false;

  if (Call->getIntrinsicID() != Intrinsic::not_intrinsic)
    return false;

  if (Call->doesNotThrow() &&
      (Call->doesNotAccessMemory() || Call->onlyReadsMemory()))
    return false;

  const Function *Callee = Call->getCalledFunction();
  if (Callee && isNonSafepointFunction(*Callee))
    return false;

  return true;
}

static bool isGCReferenceType(Type *Ty) {
  if (auto *PT = dyn_cast<PointerType>(Ty))
    return PT->getAddressSpace() == GCAddressSpace;
  return false;
}

static bool containsGCReferenceType(Type *Ty) {
  if (isGCReferenceType(Ty))
    return true;
  for (Type *Sub : Ty->subtypes()) {
    if (containsGCReferenceType(Sub))
      return true;
  }
  return false;
}

static bool isInteriorPointerCall(const Value *V) {
  auto *CB = dyn_cast<CallBase>(V);
  if (!CB)
    return false;
  const Function *Callee = CB->getCalledFunction();
  return Callee && isInteriorPointerFunction(*Callee);
}

static bool tracesToGCReference(Value *V, SmallPtrSetImpl<Value *> &Visited) {
  if (!Visited.insert(V).second)
    return false;
  if (auto *ASC = dyn_cast<AddrSpaceCastInst>(V))
    return isGCReferenceType(ASC->getSrcTy()) ||
           tracesToGCReference(ASC->getPointerOperand(), Visited);
  if (auto *GEP = dyn_cast<GetElementPtrInst>(V))
    return isGCReferenceType(GEP->getPointerOperandType()) ||
           tracesToGCReference(GEP->getPointerOperand(), Visited);
  if (isInteriorPointerCall(V))
    return true;
  if (auto *Phi = dyn_cast<PHINode>(V)) {
    for (Value *Inc : Phi->incoming_values()) {
      if (tracesToGCReference(Inc, Visited))
        return true;
    }
    return false;
  }
  if (auto *Sel = dyn_cast<SelectInst>(V))
    return tracesToGCReference(Sel->getTrueValue(), Visited) ||
           tracesToGCReference(Sel->getFalseValue(), Visited);
  return false;
}

static bool isDerivedPointer(Value *V, SmallPtrSetImpl<Value *> &Visited) {
  if (!Visited.insert(V).second)
    return false;

  if (isa<GetElementPtrInst>(V) || isa<AllocaInst>(V))
    return true;

  if (auto *CE = dyn_cast<ConstantExpr>(V)) {
    return CE->getOpcode() == Instruction::GetElementPtr ||
           CE->getOpcode() == Instruction::IntToPtr;
  }

  if (auto *ITP = dyn_cast<IntToPtrInst>(V))
    return !isGCReferenceType(ITP->getType());

  if (auto *ASC = dyn_cast<AddrSpaceCastInst>(V)) {
    if (isGCReferenceType(ASC->getSrcTy()))
      return true;
    SmallPtrSet<Value *, 8> TraceVisited;
    return tracesToGCReference(ASC->getPointerOperand(), TraceVisited);
  }

  if (isInteriorPointerCall(V))
    return true;

  if (auto *Phi = dyn_cast<PHINode>(V)) {
    for (Value *Inc : Phi->incoming_values()) {
      if (isDerivedPointer(Inc, Visited))
        return true;
    }
    return false;
  }

  if (auto *Sel = dyn_cast<SelectInst>(V)) {
    return isDerivedPointer(Sel->getTrueValue(), Visited) ||
           isDerivedPointer(Sel->getFalseValue(), Visited);
  }

  return false;
}

static bool isDerivedPointer(Value *V) {
  SmallPtrSet<Value *, 8> Visited;
  return isDerivedPointer(V, Visited);
}

static Value *stripOffsets(Value *V) {
  while (auto *GEP = dyn_cast<GetElementPtrInst>(V))
    V = GEP->getPointerOperand();
  return V;
}

static bool isCandidateRoot(Value *V) {
  if (!isGCReferenceType(V->getType()))
    return false;
  if (isa<Constant>(V))
    return false;
  if (isDerivedPointer(V))
    return false;
  return true;
}

static bool isLaunderedPlainObject(Value *V,
                                   const SmallSetVector<Value *, 8> &PlainRoots) {
  if (!V->getType()->isPointerTy() || isGCReferenceType(V->getType()))
    return false;
  if (!isa<Instruction>(V) && !isa<Argument>(V))
    return false;
  if (PlainRoots.contains(V))
    return true;
  if (isa<AllocaInst>(V))
    return false;
  SmallPtrSet<Value *, 8> Visited;
  if (tracesToGCReference(V, Visited))
    return false;
  return llvm::any_of(V->users(), [](User *U) {
    auto *ASC = dyn_cast<AddrSpaceCastInst>(U);
    return ASC && isGCReferenceType(ASC->getType()) && isCandidateRoot(ASC);
  });
}

static bool
tracesToLaunderedPlainObject(Value *V, SmallPtrSetImpl<Value *> &Visited,
                             const SmallSetVector<Value *, 8> &PlainRoots) {
  if (!Visited.insert(V).second)
    return false;
  if (isLaunderedPlainObject(V, PlainRoots))
    return true;
  if (auto *GEP = dyn_cast<GetElementPtrInst>(V))
    return tracesToLaunderedPlainObject(GEP->getPointerOperand(), Visited,
                                        PlainRoots);
  if (auto *Phi = dyn_cast<PHINode>(V)) {
    for (Value *Inc : Phi->incoming_values()) {
      if (tracesToLaunderedPlainObject(Inc, Visited, PlainRoots))
        return true;
    }
    return false;
  }
  if (auto *Sel = dyn_cast<SelectInst>(V))
    return tracesToLaunderedPlainObject(Sel->getTrueValue(), Visited,
                                        PlainRoots) ||
           tracesToLaunderedPlainObject(Sel->getFalseValue(), Visited,
                                        PlainRoots);
  return false;
}

static std::optional<BasicBlock::iterator> insertionPointAfterDef(Value *V) {
  if (auto *Arg = dyn_cast<Argument>(V))
    return Arg->getParent()->getEntryBlock().getFirstInsertionPt();
  auto *I = cast<Instruction>(V);
  if (isa<PHINode>(I))
    return I->getParent()->getFirstInsertionPt();
  if (auto *II = dyn_cast<InvokeInst>(I)) {
    BasicBlock *Normal = II->getNormalDest();
    if (!Normal->getSinglePredecessor())
      return std::nullopt;
    return Normal->getFirstInsertionPt();
  }
  if (I->isTerminator())
    return std::nullopt;
  return std::next(I->getIterator());
}

static void
splitPlainRootInvokeEdges(const SmallSetVector<Value *, 8> &PlainRoots,
                          DominatorTree &DT) {
  for (Value *V : PlainRoots) {
    auto *II = dyn_cast<InvokeInst>(V);
    if (!II)
      continue;
    BasicBlock *Normal = II->getNormalDest();
    if (Normal->getSinglePredecessor())
      continue;
    SplitEdge(II->getParent(), Normal, &DT);
  }
}

class MergeBases {
public:
  explicit MergeBases(Function &F, const DominatorTree &DT,
                      const SmallSetVector<Value *, 8> &PlainRoots)
      : GCNull(ConstantPointerNull::get(
            PointerType::get(F.getContext(), GCAddressSpace))),
        DT(DT), PlainRoots(PlainRoots) {
    SmallVector<Instruction *, 16> Merges;
    SmallPtrSet<Instruction *, 8> LaunderedMerges;
    for (BasicBlock &BB : F) {
      if (!DT.isReachableFromEntry(&BB))
        continue;
      for (Instruction &I : BB) {
        if (!isa<PHINode>(I) && !isa<SelectInst>(I))
          continue;
        if (!I.getType()->isPointerTy() || isCandidateRoot(&I))
          continue;
        SmallPtrSet<Value *, 8> Visited;
        SmallPtrSet<Value *, 8> LaunderedVisited;
        if (isGCReferenceType(I.getType()) || tracesToGCReference(&I, Visited)) {
          Merges.push_back(&I);
        } else if (tracesToLaunderedPlainObject(&I, LaunderedVisited,
                                                PlainRoots)) {
          Merges.push_back(&I);
          LaunderedMerges.insert(&I);
        }
      }
    }
    for (Instruction *M : Merges) {
      Value *Base = findBase(M);
      auto *BaseInst = dyn_cast_or_null<Instruction>(Base);
      if (BaseInst &&
          (Synthesized.contains(BaseInst) || LaunderedMerges.contains(M))) {
        BaseOf[M] = BaseInst;
        MergesOf[BaseInst].push_back(M);
      }
    }
  }

  Value *baseOf(Value *V) const { return BaseOf.lookup(V); }

  bool isSynthesized(const Value *V) const {
    auto *I = dyn_cast<Instruction>(V);
    return I && Synthesized.contains(I);
  }

  ArrayRef<Value *> mergesOf(Value *Base) const {
    auto It = MergesOf.find(Base);
    return It == MergesOf.end() ? ArrayRef<Value *>() : ArrayRef(It->second);
  }

  void eraseUnused() {
    SmallPtrSet<Instruction *, 8> Live;
    SmallVector<Instruction *, 8> Worklist;
    for (Instruction *I : SynthesizedOrder) {
      bool UsedElsewhere = llvm::any_of(I->users(), [&](User *U) {
        auto *UI = dyn_cast<Instruction>(U);
        return !UI || !Synthesized.contains(UI);
      });
      if (UsedElsewhere && Live.insert(I).second)
        Worklist.push_back(I);
    }
    while (!Worklist.empty()) {
      Instruction *I = Worklist.pop_back_val();
      for (Value *Op : I->operands()) {
        auto *OpI = dyn_cast<Instruction>(Op);
        if (OpI && Synthesized.contains(OpI) && Live.insert(OpI).second)
          Worklist.push_back(OpI);
      }
    }
    SmallVector<Instruction *, 8> Dead;
    for (Instruction *I : SynthesizedOrder) {
      if (!Live.contains(I))
        Dead.push_back(I);
    }
    for (Instruction *I : Dead)
      I->dropAllReferences();
    for (Instruction *I : Dead) {
      Synthesized.erase(I);
      I->eraseFromParent();
    }
    SynthesizedOrder.clear();
  }

private:
  Value *findBase(Value *V) {
    auto It = Cache.find(V);
    if (It != Cache.end())
      return It->second;
    Value *Base = computeBase(V);
    Cache[V] = Base;
    return Base;
  }

  Value *computeBase(Value *V) {
    if (isCandidateRoot(V))
      return V;
    if (PlainRoots.contains(V))
      return launderedBase(V);
    auto *I = dyn_cast<Instruction>(V);
    if (!I)
      return GCNull;
    if (auto *GEP = dyn_cast<GetElementPtrInst>(I))
      return findBase(GEP->getPointerOperand());
    if (auto *ASC = dyn_cast<AddrSpaceCastInst>(I))
      return findBase(ASC->getPointerOperand());
    if (isInteriorPointerCall(I))
      return findBase(cast<CallBase>(I)->getArgOperand(0));
    if (auto *Phi = dyn_cast<PHINode>(I))
      return mergeBases(Phi);
    if (auto *Sel = dyn_cast<SelectInst>(I))
      return mergeBases(Sel);
    return GCNull;
  }

  Value *launderedBase(Value *X) {
    std::optional<BasicBlock::iterator> InsertPt = insertionPointAfterDef(X);
    if (!InsertPt)
      return GCNull;
    auto *Cast = new AddrSpaceCastInst(X, GCNull->getType(),
                                       X->getName() + ".base", *InsertPt);
    for (User *U : make_early_inc_range(X->users())) {
      auto *ASC = dyn_cast<AddrSpaceCastInst>(U);
      if (ASC && ASC != Cast && isGCReferenceType(ASC->getType()) &&
          DT.dominates(Cast, ASC))
        ASC->replaceAllUsesWith(Cast);
    }
    Synthesized.insert(Cast);
    SynthesizedOrder.push_back(Cast);
    return Cast;
  }

  Value *findBaseAt(Value *V, Instruction *At) {
    Value *X = V;
    while (true) {
      if (PlainRoots.contains(X))
        return findBase(V);
      auto *GEP = dyn_cast<GetElementPtrInst>(X);
      if (!GEP)
        break;
      X = GEP->getPointerOperand();
    }
    if (!isLaunderedPlainObject(X, PlainRoots))
      return findBase(V);
    for (User *U : X->users()) {
      auto *ASC = dyn_cast<AddrSpaceCastInst>(U);
      if (ASC && isGCReferenceType(ASC->getType()) && isCandidateRoot(ASC) &&
          DT.isReachableFromEntry(ASC->getParent()) && DT.dominates(ASC, At))
        return ASC;
    }
    return GCNull;
  }

  template <typename MergeT> Value *mergeBases(MergeT *M) {
    Instruction *Placeholder;
    if constexpr (std::is_same_v<MergeT, PHINode>)
      Placeholder = PHINode::Create(GCNull->getType(), M->getNumIncomingValues(),
                                    M->getName() + ".base", M->getIterator());
    else
      Placeholder = SelectInst::Create(M->getCondition(), GCNull, GCNull,
                                       M->getName() + ".base", M->getIterator());
    Cache[M] = Placeholder;

    SmallVector<Value *, 4> Bases;
    if constexpr (std::is_same_v<MergeT, PHINode>) {
      for (unsigned I = 0; I < M->getNumIncomingValues(); ++I)
        Bases.push_back(findBaseAt(M->getIncomingValue(I),
                                   M->getIncomingBlock(I)->getTerminator()));
    } else {
      Bases.push_back(findBaseAt(M->getTrueValue(), M));
      Bases.push_back(findBaseAt(M->getFalseValue(), M));
    }

    Value *Unique = nullptr;
    bool IsUnique = true;
    for (Value *B : Bases) {
      if (B == Placeholder || B == Unique)
        continue;
      if (Unique) {
        IsUnique = false;
        break;
      }
      Unique = B;
    }
    if (auto *UniqueInst = dyn_cast_or_null<Instruction>(Unique);
        UniqueInst && !DT.dominates(UniqueInst, M))
      IsUnique = false;
    if (IsUnique) {
      Value *Result = Unique ? Unique : GCNull;
      Placeholder->replaceAllUsesWith(Result);
      Placeholder->eraseFromParent();
      Cache[M] = Result;
      return Result;
    }

    if constexpr (std::is_same_v<MergeT, PHINode>) {
      auto *BasePhi = cast<PHINode>(Placeholder);
      for (unsigned I = 0; I < M->getNumIncomingValues(); ++I)
        BasePhi->addIncoming(Bases[I], M->getIncomingBlock(I));
    } else {
      Placeholder->setOperand(1, Bases[0]);
      Placeholder->setOperand(2, Bases[1]);
    }
    Synthesized.insert(Placeholder);
    SynthesizedOrder.push_back(Placeholder);
    return Placeholder;
  }

  Constant *GCNull;
  const DominatorTree &DT;
  const SmallSetVector<Value *, 8> &PlainRoots;
  DenseMap<Value *, WeakTrackingVH> Cache;
  SmallPtrSet<Instruction *, 8> Synthesized;
  SmallVector<Instruction *, 8> SynthesizedOrder;
  DenseMap<Value *, Value *> BaseOf;
  DenseMap<Value *, SmallVector<Value *, 2>> MergesOf;
};

class RootLiveness {
public:
  RootLiveness(Value *V, const DominatorTree &DT,
               const MergeBases *Bases = nullptr, bool IsPlainRoot = false)
      : V(V), DT(DT), Bases(Bases), IsPlainRoot(IsPlainRoot) {
    if (auto *DefInst = dyn_cast<Instruction>(V))
      DefBB = DefInst->getParent();
    collectEffectiveUses();
    computeLiveIn();
  }

  bool isLiveAcross(CallBase *S) const {
    BasicBlock *SBB = S->getParent();
    if (!DT.isReachableFromEntry(SBB))
      return false;

    if (auto *DefInst = dyn_cast<Instruction>(V)) {
      if (DefInst == S)
        return false;
      if (DefBB == SBB) {
        if (!DefInst->comesBefore(S))
          return false;
      } else if (!DT.dominates(DefInst, S)) {
        return false;
      }
    } else if (!isa<Argument>(V)) {
      return false;
    }

    if (PhiEdgeBlocks.contains(SBB))
      return true;
    auto It = UsesInBlock.find(SBB);
    if (It != UsesInBlock.end()) {
      for (Instruction *UI : It->second) {
        if (UI == S || S->comesBefore(UI))
          return true;
      }
    }

    for (BasicBlock *Succ : successors(SBB)) {
      if (LiveIn.contains(Succ))
        return true;
    }
    return false;
  }

private:
  void collectEffectiveUses() {
    SmallVector<Value *, 8> Worklist;
    SmallPtrSet<Value *, 16> Visited;
    Worklist.push_back(V);
    Visited.insert(V);
    if (Bases) {
      for (Value *Merge : Bases->mergesOf(V)) {
        if (Visited.insert(Merge).second)
          Worklist.push_back(Merge);
      }
    }
    collectUses(Worklist, Visited, nullptr);

    Value *Plain = nullptr;
    if (IsPlainRoot)
      Plain = V;
    else if (auto *ASC = dyn_cast<AddrSpaceCastInst>(V);
             ASC && !isGCReferenceType(ASC->getSrcTy()))
      Plain = ASC->getPointerOperand();
    if (!Plain)
      return;
    Value *Base = stripOffsets(Plain);
    if (!isa<Instruction>(Base) && !isa<Argument>(Base))
      return;
    if (Visited.insert(Base).second) {
      Worklist.push_back(Base);
      collectUses(Worklist, Visited, cast<Instruction>(V));
    }
  }

  void collectUses(SmallVectorImpl<Value *> &Worklist,
                   SmallPtrSetImpl<Value *> &Visited,
                   const Instruction *DominatingDef) {
    while (!Worklist.empty()) {
      Value *Curr = Worklist.pop_back_val();
      for (Use &U : Curr->uses()) {
        auto *UI = dyn_cast<Instruction>(U.getUser());
        if (!UI)
          continue;
        if (Bases) {
          if (Value *MergeBase = Bases->baseOf(UI);
              MergeBase && MergeBase != V && Bases->isSynthesized(MergeBase))
            continue;
        }
        if (IsPlainRoot && isa<AddrSpaceCastInst>(UI) &&
            isGCReferenceType(UI->getType()) &&
            DT.dominates(cast<Instruction>(V), UI)) {
          if (Visited.insert(UI).second)
            Worklist.push_back(UI);
          continue;
        }
        if (isDerivedPointer(UI)) {
          if (Visited.insert(UI).second)
            Worklist.push_back(UI);
          continue;
        }
        if (DominatingDef) {
          if (UI == V || isCandidateRoot(UI))
            continue;
          if (!DT.dominates(DominatingDef, U))
            continue;
        }
        if (auto *Phi = dyn_cast<PHINode>(UI)) {
          if (!DT.isReachableFromEntry(Phi->getParent()))
            continue;
          PhiEdgeBlocks.insert(Phi->getIncomingBlock(U));
        } else {
          UsesInBlock[UI->getParent()].push_back(UI);
        }
      }
    }
  }

  void computeLiveIn() {
    SmallVector<BasicBlock *, 16> Worklist;
    auto addLiveIn = [&](BasicBlock *BB) {
      if (BB != DefBB && DT.isReachableFromEntry(BB) && LiveIn.insert(BB).second)
        Worklist.push_back(BB);
    };
    for (BasicBlock *BB : PhiEdgeBlocks)
      addLiveIn(BB);
    for (auto &Entry : UsesInBlock)
      addLiveIn(Entry.first);
    while (!Worklist.empty()) {
      BasicBlock *BB = Worklist.pop_back_val();
      for (BasicBlock *Pred : predecessors(BB))
        addLiveIn(Pred);
    }
  }

  Value *V;
  const DominatorTree &DT;
  const MergeBases *Bases;
  bool IsPlainRoot;
  BasicBlock *DefBB = nullptr;
  DenseMap<BasicBlock *, SmallVector<Instruction *, 4>> UsesInBlock;
  SmallPtrSet<BasicBlock *, 8> PhiEdgeBlocks;
  SmallPtrSet<BasicBlock *, 16> LiveIn;
};

static Type *lowerType(Type *Ty) {
  if (auto *PT = dyn_cast<PointerType>(Ty)) {
    if (PT->getAddressSpace() == GCAddressSpace)
      return PointerType::getUnqual(Ty->getContext());
    return Ty;
  }
  if (auto *FT = dyn_cast<FunctionType>(Ty)) {
    Type *RetTy = lowerType(FT->getReturnType());
    SmallVector<Type *, 8> ParamTys;
    bool Changed = (RetTy != FT->getReturnType());
    for (Type *ParamTy : FT->params()) {
      Type *Lowered = lowerType(ParamTy);
      ParamTys.push_back(Lowered);
      if (Lowered != ParamTy)
        Changed = true;
    }
    if (Changed)
      return FunctionType::get(RetTy, ParamTys, FT->isVarArg());
    return Ty;
  }
  if (auto *AT = dyn_cast<ArrayType>(Ty)) {
    Type *ElemTy = lowerType(AT->getElementType());
    if (ElemTy != AT->getElementType())
      return ArrayType::get(ElemTy, AT->getNumElements());
    return Ty;
  }
  if (auto *VT = dyn_cast<VectorType>(Ty)) {
    Type *ElemTy = lowerType(VT->getElementType());
    if (ElemTy != VT->getElementType())
      return VectorType::get(ElemTy, VT->getElementCount());
    return Ty;
  }
  if (auto *ST = dyn_cast<StructType>(Ty)) {
    if (!ST->isLiteral())
      return Ty;
    SmallVector<Type *, 8> ElemTys;
    bool Changed = false;
    for (Type *Elem : ST->elements()) {
      Type *Lowered = lowerType(Elem);
      ElemTys.push_back(Lowered);
      if (Lowered != Elem)
        Changed = true;
    }
    if (Changed)
      return StructType::get(Ty->getContext(), ElemTys, ST->isPacked());
  }
  return Ty;
}

static bool constantMentionsGC(const Constant *C) {
  if (containsGCReferenceType(C->getType()))
    return true;
  if (isa<GlobalValue>(C))
    return false;
  for (const Use &Op : C->operands()) {
    if (auto *OpC = dyn_cast<Constant>(Op.get())) {
      if (constantMentionsGC(OpC))
        return true;
    }
  }
  return false;
}

static Constant *lowerConstant(Constant *C) {
  if (!constantMentionsGC(C))
    return C;

  Type *Ty = C->getType();
  Type *LoweredTy = lowerType(Ty);

  if (isa<ConstantPointerNull>(C))
    return ConstantPointerNull::get(cast<PointerType>(LoweredTy));
  if (isa<PoisonValue>(C))
    return PoisonValue::get(LoweredTy);
  if (isa<UndefValue>(C))
    return UndefValue::get(LoweredTy);
  if (isa<ConstantAggregateZero>(C))
    return ConstantAggregateZero::get(LoweredTy);

  SmallVector<Constant *, 8> Ops;
  for (Use &Op : C->operands())
    Ops.push_back(lowerConstant(cast<Constant>(Op.get())));

  if (isa<ConstantStruct>(C))
    return ConstantStruct::get(cast<StructType>(LoweredTy), Ops);
  if (isa<ConstantArray>(C))
    return ConstantArray::get(cast<ArrayType>(LoweredTy), Ops);
  if (isa<ConstantVector>(C))
    return ConstantVector::get(Ops);

  if (auto *CE = dyn_cast<ConstantExpr>(C)) {
    if (CE->getOpcode() == Instruction::AddrSpaceCast) {
      if (Ops[0]->getType() == LoweredTy)
        return Ops[0];
      return ConstantExpr::getAddrSpaceCast(Ops[0], LoweredTy);
    }
    Type *SrcTy = nullptr;
    if (auto *GEP = dyn_cast<GEPOperator>(CE))
      SrcTy = lowerType(GEP->getSourceElementType());
    return CE->getWithOperands(Ops, LoweredTy, false, SrcTy);
  }

  std::string Msg;
  raw_string_ostream OS(Msg);
  OS << "kotlin-build-shadow-stack: unhandled addrspace(" << GCAddressSpace
     << ") constant: " << *C;
  report_fatal_error(StringRef(OS.str()));
}

static void lowerFunctionDeclaration(Function &Decl) {
  if (!Decl.isDeclaration())
    return;

  FunctionType *CalleeFTy = Decl.getFunctionType();
  auto *LoweredCalleeFTy = cast<FunctionType>(lowerType(CalleeFTy));
  if (LoweredCalleeFTy == CalleeFTy)
    return;

  Module *M = Decl.getParent();
  std::string Name = Decl.getName().str();
  Decl.setName("");
  Function *NewDecl = Function::Create(LoweredCalleeFTy, Decl.getLinkage(),
                                       Decl.getAddressSpace(), "");
  M->getFunctionList().insert(Decl.getIterator(), NewDecl);
  NewDecl->setName(Name);
  NewDecl->copyAttributesFrom(&Decl);
  Decl.replaceAllUsesWith(NewDecl);
  Decl.eraseFromParent();

  if (NewDecl->isIntrinsic()) {
    if (std::optional<Function *> Remangled =
            Intrinsic::remangleIntrinsicFunction(NewDecl)) {
      NewDecl->replaceAllUsesWith(*Remangled);
      NewDecl->eraseFromParent();
    }
  }
}

static void lowerAddressSpaces(Function &F) {
  SmallVector<AddrSpaceCastInst *, 8> GCCasts;
  for (BasicBlock &BB : F) {
    for (Instruction &I : BB) {
      if (auto *ASC = dyn_cast<AddrSpaceCastInst>(&I)) {
        if (isGCReferenceType(ASC->getSrcTy()) ||
            isGCReferenceType(ASC->getDestTy()))
          GCCasts.push_back(ASC);
      }
    }
  }

  FunctionType *FTy = F.getFunctionType();
  auto *LoweredFTy = cast<FunctionType>(lowerType(FTy));
  if (LoweredFTy != FTy)
    setFunctionType(F, LoweredFTy);

  for (Argument &Arg : F.args()) {
    if (containsGCReferenceType(Arg.getType()))
      Arg.mutateType(lowerType(Arg.getType()));
  }

  for (BasicBlock &BB : F) {
    for (Instruction &I : BB) {
      if (containsGCReferenceType(I.getType()))
        I.mutateType(lowerType(I.getType()));
    }
  }

  for (BasicBlock &BB : F) {
    for (Instruction &I : BB) {
      for (unsigned i = 0; i < I.getNumOperands(); ++i) {
        if (auto *C = dyn_cast<Constant>(I.getOperand(i))) {
          if (!isa<GlobalValue>(C) && constantMentionsGC(C))
            I.setOperand(i, lowerConstant(C));
        }
      }
    }
  }

  for (AddrSpaceCastInst *ASC : GCCasts) {
    Value *Op = ASC->getPointerOperand();
    if (Op->getType() == ASC->getType()) {
      ASC->replaceAllUsesWith(Op);
      ASC->eraseFromParent();
    }
  }

  for (BasicBlock &BB : F) {
    for (Instruction &I : BB) {
      if (auto *AI = dyn_cast<AllocaInst>(&I)) {
        AI->setAllocatedType(lowerType(AI->getAllocatedType()));
      }
      if (auto *GEP = dyn_cast<GetElementPtrInst>(&I)) {
        GEP->setSourceElementType(lowerType(GEP->getSourceElementType()));
        SmallVector<Value *, 4> Indices(GEP->indices());
        GEP->setResultElementType(GetElementPtrInst::getIndexedType(
            GEP->getSourceElementType(), Indices));
      }
      if (auto *CB = dyn_cast<CallBase>(&I)) {
        FunctionType *CFTy = CB->getFunctionType();
        FunctionType *LoweredCFTy = cast<FunctionType>(lowerType(CFTy));
        if (LoweredCFTy != CFTy) {
          CB->mutateFunctionType(LoweredCFTy);
        }
      }
    }
  }
}

static void lowerModuleDeclarations(Module &M) {
  SmallVector<Function *, 16> DeclsToLower;
  for (Function &Decl : M.functions()) {
    if (Decl.isDeclaration())
      DeclsToLower.push_back(&Decl);
  }
  for (Function *Decl : DeclsToLower)
    lowerFunctionDeclaration(*Decl);
}

static Instruction *getStorePoint(Value *V, BasicBlock &EntryBB) {
  if (isa<Argument>(V)) {
    BasicBlock::iterator FirstNonAlloca = EntryBB.begin();
    while (FirstNonAlloca != EntryBB.end() && isa<AllocaInst>(*FirstNonAlloca)) {
      ++FirstNonAlloca;
    }
    return &*FirstNonAlloca;
  }
  if (auto *Phi = dyn_cast<PHINode>(V)) {
    return &*Phi->getParent()->getFirstInsertionPt();
  }
  if (auto *II = dyn_cast<InvokeInst>(V)) {
    return &*II->getNormalDest()->getFirstInsertionPt();
  }
  if (auto *Inst = dyn_cast<Instruction>(V)) {
    return Inst->getNextNode();
  }
  return nullptr;
}

static bool canStoreOverwrite(Instruction *Store2, Value *V1, CallBase *S1,
                              BasicBlock &EntryBB, const DominatorTree &DT) {
  Instruction *Store1 = getStorePoint(V1, EntryBB);
  if (!Store1)
    return true;

  if (isa<Argument>(V1)) {
    return isPotentiallyReachable(Store2, S1, nullptr, &DT);
  }

  BasicBlock *DefBB = Store1->getParent();
  BasicBlock *SBB = S1->getParent();
  if (DefBB == SBB && (Store1 == S1 || Store1->comesBefore(S1))) {
    if (Store2->getParent() == DefBB &&
        Store1->comesBefore(Store2) && Store2->comesBefore(S1)) {
      return true;
    }
    return false;
  }

  SmallPtrSet<BasicBlock *, 4> ExclusionSet;
  ExclusionSet.insert(DefBB);
  return isPotentiallyReachable(Store2, S1, &ExclusionSet, &DT);
}

static bool rootsInterfere(
    Value *V1, Value *V2,
    const DenseMap<Value *, SmallVector<CallBase *, 8>> &SafepointsForRoot,
    BasicBlock &EntryBB, const DominatorTree &DT) {
  auto It1 = SafepointsForRoot.find(V1);
  auto It2 = SafepointsForRoot.find(V2);
  assert(It1 != SafepointsForRoot.end() && It2 != SafepointsForRoot.end());
  const auto &SP1 = It1->second;
  const auto &SP2 = It2->second;

  for (CallBase *S : SP1) {
    if (llvm::is_contained(SP2, S))
      return true;
  }

  if (isa<Argument>(V1) && isa<Argument>(V2))
    return true;

  Instruction *Store1 = getStorePoint(V1, EntryBB);
  Instruction *Store2 = getStorePoint(V2, EntryBB);
  if (!Store1 || !Store2)
    return true;

  if (isPotentiallyReachable(Store1, Store2, nullptr, &DT)) {
    for (CallBase *S1 : SP1) {
      if (canStoreOverwrite(Store2, V1, S1, EntryBB, DT))
        return true;
    }
  }

  if (isPotentiallyReachable(Store2, Store1, nullptr, &DT)) {
    for (CallBase *S2 : SP2) {
      if (canStoreOverwrite(Store1, V2, S2, EntryBB, DT))
        return true;
    }
  }

  return false;
}

static bool hasGCAddressSpaceValue(const Function &F) {
  if (containsGCReferenceType(F.getFunctionType()))
    return true;
  for (const BasicBlock &BB : F) {
    for (const Instruction &I : BB) {
      if (containsGCReferenceType(I.getType()))
        return true;
      for (const Value *Op : I.operand_values()) {
        if (containsGCReferenceType(Op->getType()))
          return true;
        if (auto *C = dyn_cast<Constant>(Op); C && constantMentionsGC(C))
          return true;
      }
    }
  }
  return false;
}

static bool buildShadowStack(Function &F, DominatorTree *DT = nullptr) {
  if (F.isDeclaration() || F.empty())
    return false;

  if (!F.hasFnAttribute(KotlinGcFrameAttrName)) {
    if (hasGCAddressSpaceValue(F)) {
      std::string Msg;
      raw_string_ostream OS(Msg);
      OS << "kotlin-build-shadow-stack: function '" << F.getName()
         << "' holds addrspace(" << GCAddressSpace << ") values but is not marked '"
         << KotlinGcFrameAttrName << "'";
      report_fatal_error(StringRef(Msg));
    }
    lowerAddressSpaces(F);
    return false;
  }

  for (BasicBlock &BB : F) {
    if (BB.isEHPad() && !BB.isLandingPad()) {
      std::string Msg;
      raw_string_ostream OS(Msg);
      OS << "kotlin-build-shadow-stack: funclet-based exception handling is not "
            "supported, in function '"
         << F.getName() << "'";
      report_fatal_error(StringRef(Msg));
    }
  }

  std::unique_ptr<DominatorTree> LocalDT;
  if (!DT) {
    LocalDT = std::make_unique<DominatorTree>(F);
    DT = LocalDT.get();
  }

  SmallVector<AllocaInst *, 16> PromotableAllocas;
  for (Instruction &I : F.getEntryBlock()) {
    if (auto *AI = dyn_cast<AllocaInst>(&I)) {
      if (isAllocaPromotable(AI))
        PromotableAllocas.push_back(AI);
    }
  }
  if (!PromotableAllocas.empty()) {
    PromoteMemToReg(PromotableAllocas, *DT);
  }

  SmallVector<CallBase *, 16> Safepoints;
  for (BasicBlock &BB : F) {
    if (!DT->isReachableFromEntry(&BB))
      continue;
    for (Instruction &I : BB) {
      if (isSafepointOrAllocationCall(I)) {
        Safepoints.push_back(cast<CallBase>(&I));
      }
    }
  }

  SmallVector<CallInst *, 4> ReturnSlotMarkers;
  collectMarkerCalls(F, ReturnSlotMarkerName, ReturnSlotMarkers);

  SmallVector<CallInst *, 1> FrameEnterMarkers;
  SmallVector<CallInst *, 4> FrameLeaveMarkers;
  SmallVector<CallInst *, 4> FrameSetCurrentMarkers;
  collectMarkerCalls(F, FrameEnterMarkerName, FrameEnterMarkers);
  collectMarkerCalls(F, FrameLeaveMarkerName, FrameLeaveMarkers);
  collectMarkerCalls(F, FrameSetCurrentMarkerName, FrameSetCurrentMarkers);
  SmallVector<CallInst *, 4> KeepAliveMarkers;
  collectMarkerCalls(F, KeepAliveMarkerName, KeepAliveMarkers);

  SmallVector<CallInst *, 4> StackObjectMarkers;
  if (Function *Marker = F.getParent()->getFunction(StackObjectMarkerName);
      Marker && !Marker->use_empty()) {
    for (Instruction &I : instructions(F)) {
      auto *CI = dyn_cast<CallInst>(&I);
      if (CI && CI->getCalledOperand() == Marker)
        StackObjectMarkers.push_back(CI);
    }
  }

  auto eraseFrameMarkers = [&]() {
    for (CallInst *CI : ReturnSlotMarkers) {
      assert(CI->use_empty() && "return slot marker must have been lowered");
      CI->eraseFromParent();
    }
    for (CallInst *CI : FrameEnterMarkers)
      CI->eraseFromParent();
    for (CallInst *CI : FrameLeaveMarkers)
      CI->eraseFromParent();
    for (CallInst *CI : FrameSetCurrentMarkers)
      CI->eraseFromParent();
    for (CallInst *CI : StackObjectMarkers)
      CI->eraseFromParent();
    for (CallInst *CI : KeepAliveMarkers)
      CI->eraseFromParent();
  };

  if (FrameEnterMarkers.size() > 1) {
    std::string Msg;
    raw_string_ostream OS(Msg);
    OS << "kotlin-build-shadow-stack: function '" << F.getName() << "' has "
       << FrameEnterMarkers.size() << " " << FrameEnterMarkerName
       << " markers, expected at most one";
    report_fatal_error(StringRef(Msg));
  }

  SmallSetVector<Value *, 8> PlainRoots;
  DenseMap<Value *, SmallVector<Instruction *, 1>> PlainRootsByBase;
  for (CallInst *CI : ReturnSlotMarkers) {
    for (User *U : CI->users()) {
      Instruction *X = nullptr;
      if (auto *SI = dyn_cast<StoreInst>(U);
          SI && SI->getPointerOperand() == CI)
        X = dyn_cast<Instruction>(SI->getValueOperand());
      else if (auto *Call = dyn_cast<CallBase>(U); Call && Call->hasArgument(CI))
        X = Call;
      if (!X || !X->getType()->isPointerTy() ||
          isGCReferenceType(X->getType()) ||
          !DT->isReachableFromEntry(X->getParent()))
        continue;
      SmallPtrSet<Value *, 8> Visited;
      if (tracesToGCReference(X, Visited))
        continue;
      if (PlainRoots.insert(X))
        PlainRootsByBase[stripOffsets(X)].push_back(X);
    }
  }

  splitPlainRootInvokeEdges(PlainRoots, *DT);
  MergeBases Bases(F, *DT, PlainRoots);

  auto isCoveredByPlainRoot = [&](Value *V) {
    auto *ASC = dyn_cast<AddrSpaceCastInst>(V);
    if (!ASC || isGCReferenceType(ASC->getSrcTy()) || Bases.isSynthesized(ASC))
      return false;
    auto It = PlainRootsByBase.find(stripOffsets(ASC->getPointerOperand()));
    return It != PlainRootsByBase.end() &&
           llvm::any_of(It->second, [&](Instruction *Root) {
             return DT->dominates(Root, ASC);
           });
  };

  SmallVector<Value *, 16> CandidateRoots;
  SmallVector<Value *, 4> AggregateRoots;
  auto classify = [&](Value *V) {
    if (isCandidateRoot(V)) {
      if (!isCoveredByPlainRoot(V))
        CandidateRoots.push_back(V);
    } else if (!V->getType()->isPointerTy() &&
               containsGCReferenceType(V->getType())) {
      AggregateRoots.push_back(V);
    }
  };
  for (Argument &Arg : F.args())
    classify(&Arg);
  for (BasicBlock &BB : F) {
    if (!DT->isReachableFromEntry(&BB))
      continue;
    for (Instruction &I : BB)
      classify(&I);
  }
  CandidateRoots.append(PlainRoots.begin(), PlainRoots.end());

  for (Value *V : AggregateRoots) {
    RootLiveness Liveness(V, *DT);
    for (CallBase *S : Safepoints) {
      if (!Liveness.isLiveAcross(S))
        continue;
      std::string Msg;
      raw_string_ostream OS(Msg);
      OS << "kotlin-build-shadow-stack: in function '" << F.getName()
         << "', a value of type " << *V->getType()
         << " holds references across a safepoint and cannot be rooted";
      report_fatal_error(StringRef(Msg));
    }
  }

  SmallVector<Value *, 16> LiveRoots;
  DenseMap<Value *, SmallVector<CallBase *, 8>> SafepointsForRoot;
  bool IsEntryFromC = !FrameEnterMarkers.empty();

  for (Value *V : CandidateRoots) {
    if (isa<Argument>(V) && !IsEntryFromC)
      continue;
    RootLiveness Liveness(V, *DT, &Bases, PlainRoots.contains(V));
    SmallVector<CallBase *, 8> CoveringSafepoints;
    for (CallBase *S : Safepoints) {
      if (Liveness.isLiveAcross(S))
        CoveringSafepoints.push_back(S);
    }
    if (!CoveringSafepoints.empty()) {
      SafepointsForRoot[V] = std::move(CoveringSafepoints);
      LiveRoots.push_back(V);
    }
  }

  if (LiveRoots.empty() && ReturnSlotMarkers.empty() &&
      StackObjectMarkers.empty() && FrameSetCurrentMarkers.empty()) {
    eraseFrameMarkers();
    Bases.eraseUnused();
    lowerAddressSpaces(F);
    return false;
  }

  DenseMap<Value *, SmallPtrSet<Value *, 8>> InterferenceGraph;
  for (size_t i = 0; i < LiveRoots.size(); ++i) {
    for (size_t j = i + 1; j < LiveRoots.size(); ++j) {
      Value *V1 = LiveRoots[i];
      Value *V2 = LiveRoots[j];
      if (rootsInterfere(V1, V2, SafepointsForRoot, F.getEntryBlock(), *DT)) {
        InterferenceGraph[V1].insert(V2);
        InterferenceGraph[V2].insert(V1);
      }
    }
  }

  SmallVector<Value *, 16> OrderedRoots = LiveRoots;
  std::sort(OrderedRoots.begin(), OrderedRoots.end(), [&](Value *A, Value *B) {
    return InterferenceGraph[A].size() > InterferenceGraph[B].size();
  });

  DenseMap<Value *, unsigned> SlotAssignment;
  unsigned MaxSlot = 0;
  for (Value *V : OrderedRoots) {
    DenseSet<unsigned> UsedColors;
    for (Value *Neighbor : InterferenceGraph[V]) {
      auto It = SlotAssignment.find(Neighbor);
      if (It != SlotAssignment.end())
        UsedColors.insert(It->second);
    }
    unsigned Color = 0;
    while (UsedColors.contains(Color))
      ++Color;
    SlotAssignment[V] = Color;
    if (Color > MaxSlot)
      MaxSlot = Color;
  }

  unsigned NumRootSlots = LiveRoots.empty() ? 0 : MaxSlot + 1;

  unsigned ReturnSlotIdx = NumRootSlots;
  unsigned NumSlots = NumRootSlots + (ReturnSlotMarkers.empty() ? 0 : 1);

  DenseMap<Value *, unsigned> StackObjectSlots;
  for (CallInst *CI : StackObjectMarkers) {
    Value *Obj = getUnderlyingObject(CI->getArgOperand(0));
    if (StackObjectSlots.insert({Obj, NumSlots}).second)
      ++NumSlots;
  }

  Module *Mod = F.getParent();
  unsigned OverlaySlots = getFrameOverlaySlots(*Mod);
  unsigned TotalSlots = OverlaySlots + NumSlots;

  BasicBlock &EntryBB = F.getEntryBlock();
  auto &Ctx = F.getContext();
  const DataLayout &DL = Mod->getDataLayout();
  Type *Ptr0Ty = PointerType::getUnqual(Ctx);
  Type *Int32Ty = Type::getInt32Ty(Ctx);

  DebugLoc FallbackDL;
  if (DISubprogram *SP = F.getSubprogram())
    FallbackDL = DILocation::get(Ctx, 0, 0, SP);
  auto withDebugLoc = [&](Instruction *I) {
    if (!I->getDebugLoc())
      I->setDebugLoc(FallbackDL);
    return I;
  };

  BasicBlock::iterator FirstNonAlloca = EntryBB.begin();
  while (FirstNonAlloca != EntryBB.end() && isa<AllocaInst>(*FirstNonAlloca)) {
    ++FirstNonAlloca;
  }
  IRBuilder<> EntryBuilder(&EntryBB, FirstNonAlloca);

  ArrayType *FrameArrayTy = ArrayType::get(Ptr0Ty, TotalSlots);
  AllocaInst *FrameAlloca =
      EntryBuilder.CreateAlloca(FrameArrayTy, nullptr, "shadow_stack_frame");
  FrameAlloca->setAlignment(Align(8));

  for (BasicBlock &BB : F) {
    for (Instruction &I : BB) {
      auto *CI = dyn_cast<CallInst>(&I);
      if (CI && CI->isMustTailCall()) {
        std::string Msg;
        raw_string_ostream OS(Msg);
        OS << "kotlin-build-shadow-stack: function '" << F.getName()
           << "' needs a frame but contains a musttail call";
        report_fatal_error(StringRef(Msg));
      }
    }
  }

  uint64_t FrameBytes = TotalSlots * DL.getPointerSize();
  withDebugLoc(EntryBuilder.CreateMemSet(
      FrameAlloca,
      ConstantInt::get(Type::getInt8Ty(Ctx), 0),
      FrameBytes,
      Align(8)));

  FunctionCallee EnterFrameFunc = getOrCreateEnterFrame(*Mod);
  if (!FrameEnterMarkers.empty()) {
    IRBuilder<> EnterBuilder(FrameEnterMarkers.front());
    withDebugLoc(EnterBuilder.CreateCall(
        EnterFrameFunc,
        {FrameAlloca, ConstantInt::get(Int32Ty, 0),
         ConstantInt::get(Int32Ty, TotalSlots)}));
  } else {
    withDebugLoc(EntryBuilder.CreateCall(
        EnterFrameFunc,
        {FrameAlloca, ConstantInt::get(Int32Ty, 0),
         ConstantInt::get(Int32Ty, TotalSlots)}));
  }

  auto getSlotPtr = [&](IRBuilder<> &B, unsigned SlotIdx) -> Value * {
    return B.CreateConstInBoundsGEP2_32(
        FrameArrayTy, FrameAlloca, 0, OverlaySlots + SlotIdx,
        "slot_" + Twine(SlotIdx));
  };

  Value *ReturnSlotPtr = nullptr;
  if (!ReturnSlotMarkers.empty()) {
    ReturnSlotPtr = getSlotPtr(EntryBuilder, ReturnSlotIdx);
    for (CallInst *CI : ReturnSlotMarkers)
      CI->replaceAllUsesWith(ReturnSlotPtr);
  }

  DenseMap<Instruction *, unsigned> RootStores;
  for (auto [RootIdx, V] : llvm::enumerate(LiveRoots)) {
    unsigned Slot = SlotAssignment[V];

    if (auto *Arg = dyn_cast<Argument>(V)) {
      Value *SlotPtr = getSlotPtr(EntryBuilder, Slot);
      RootStores[EntryBuilder.CreateStore(Arg, SlotPtr)] = RootIdx;
    } else if (auto *Phi = dyn_cast<PHINode>(V)) {
      BasicBlock *ParentBB = Phi->getParent();
      IRBuilder<> Builder(ParentBB, ParentBB->getFirstInsertionPt());
      Value *SlotPtr = getSlotPtr(Builder, Slot);
      RootStores[Builder.CreateStore(Phi, SlotPtr)] = RootIdx;
    } else if (auto *II = dyn_cast<InvokeInst>(V)) {
      BasicBlock *NormalDest = II->getNormalDest();
      IRBuilder<> Builder(NormalDest, NormalDest->getFirstInsertionPt());
      Value *SlotPtr = getSlotPtr(Builder, Slot);
      RootStores[Builder.CreateStore(II, SlotPtr)] = RootIdx;
    } else if (auto *Inst = dyn_cast<Instruction>(V)) {
      Instruction *NextInst = Inst->getNextNode();
      assert(NextInst && "Non-terminator instruction must have next node");
      IRBuilder<> Builder(NextInst);
      Value *SlotPtr = getSlotPtr(Builder, Slot);
      RootStores[Builder.CreateStore(Inst, SlotPtr)] = RootIdx;
    }
  }

  for (CallInst *CI : StackObjectMarkers) {
    Value *Header = CI->getArgOperand(0);
    unsigned Slot = StackObjectSlots.lookup(getUnderlyingObject(Header));
    IRBuilder<> Builder(CI);
    Builder.CreateStore(Header, getSlotPtr(Builder, Slot));
  }
  if (!StackObjectSlots.empty()) {
    Constant *Null = ConstantPointerNull::get(cast<PointerType>(Ptr0Ty));
    for (BasicBlock &BB : F) {
      for (Instruction &I : BB) {
        auto *II = dyn_cast<IntrinsicInst>(&I);
        if (!II || II->getIntrinsicID() != Intrinsic::lifetime_end)
          continue;
        Value *Obj = getUnderlyingObject(II->getArgOperand(II->arg_size() - 1));
        auto It = StackObjectSlots.find(Obj);
        if (It == StackObjectSlots.end())
          continue;
        IRBuilder<> Builder(II);
        Builder.CreateStore(Null, getSlotPtr(Builder, It->second));
      }
    }
  }

  if (NumRootSlots > 0 || ReturnSlotPtr) {
    unsigned NumRoots = LiveRoots.size();
    SmallVector<unsigned, 16> SlotOfRoot;
    for (Value *V : LiveRoots)
      SlotOfRoot.push_back(SlotAssignment[V]);
    SmallPtrSet<Instruction *, 4> ReturnSlotWriters;
    if (ReturnSlotPtr) {
      bool OnlyWritten = true;
      SmallSetVector<Value *, 4> Aliases;
      Aliases.insert(ReturnSlotPtr);
      for (unsigned I = 0; OnlyWritten && I < Aliases.size(); ++I) {
        Value *P = Aliases[I];
        for (User *U : P->users()) {
          if (isa<PHINode>(U) || isa<SelectInst>(U)) {
            Aliases.insert(U);
            continue;
          }
          auto *SI = dyn_cast<StoreInst>(U);
          bool IsWrite = (SI && SI->getPointerOperand() == P) ||
                         isa<CallBase>(U);
          if (!IsWrite) {
            OnlyWritten = false;
            break;
          }
          ReturnSlotWriters.insert(cast<Instruction>(U));
        }
      }
      auto isAlias = [&](Value *V) { return Aliases.contains(V); };
      for (Value *A : Aliases) {
        if (!OnlyWritten)
          break;
        if (auto *PN = dyn_cast<PHINode>(A))
          OnlyWritten = all_of(PN->incoming_values(), isAlias);
        else if (auto *Sel = dyn_cast<SelectInst>(A))
          OnlyWritten =
              isAlias(Sel->getTrueValue()) && isAlias(Sel->getFalseValue());
      }
      if (OnlyWritten) {
        SlotOfRoot.push_back(ReturnSlotIdx);
      } else {
        ReturnSlotWriters.clear();
      }
    }
    unsigned NumTracked = SlotOfRoot.size();
    unsigned NumTrackedSlots = ReturnSlotIdx + 1;
    SmallVector<BitVector, 16> RootsInSlot(NumTrackedSlots,
                                           BitVector(NumTracked));
    for (unsigned I = 0; I < NumTracked; ++I)
      RootsInSlot[SlotOfRoot[I]].set(I);
    DenseMap<CallBase *, BitVector> LiveAcross;
    for (CallBase *S : Safepoints)
      LiveAcross[S] = BitVector(NumTracked);
    for (unsigned I = 0; I < NumRoots; ++I) {
      for (CallBase *S : SafepointsForRoot[LiveRoots[I]])
        LiveAcross[S].set(I);
    }

    auto transfer = [&](BasicBlock &BB, BitVector &State,
                        SmallVectorImpl<std::pair<CallBase *, unsigned>> *Clears) {
      for (Instruction &I : BB) {
        auto *S = dyn_cast<CallBase>(&I);
        if (auto It = S ? LiveAcross.find(S) : LiveAcross.end();
            It != LiveAcross.end()) {
          for (unsigned Slot = 0; Slot < NumTrackedSlots; ++Slot) {
            BitVector Held = State;
            Held &= RootsInSlot[Slot];
            if (Held.none() || Held.anyCommon(It->second))
              continue;
            State.reset(RootsInSlot[Slot]);
            if (Clears)
              Clears->push_back({S, Slot});
          }
        }
        if (auto It = RootStores.find(&I); It != RootStores.end()) {
          State.reset(RootsInSlot[SlotOfRoot[It->second]]);
          State.set(It->second);
        } else if (ReturnSlotWriters.contains(&I)) {
          State.reset(RootsInSlot[ReturnSlotIdx]);
          State.set(NumRoots);
        }
      }
    };

    DenseMap<BasicBlock *, BitVector> In, Out;
    ReversePostOrderTraversal<Function *> RPOT(&F);
    for (BasicBlock *BB : RPOT) {
      In[BB] = BitVector(NumTracked);
      Out[BB] = BitVector(NumTracked);
    }
    bool Changed = true;
    while (Changed) {
      Changed = false;
      for (BasicBlock *BB : RPOT) {
        BitVector State(NumTracked);
        for (BasicBlock *Pred : predecessors(BB)) {
          if (auto It = Out.find(Pred); It != Out.end())
            State |= It->second;
        }
        In[BB] = State;
        transfer(*BB, State, nullptr);
        if (State != Out[BB]) {
          Out[BB] = std::move(State);
          Changed = true;
        }
      }
    }

    SmallVector<std::pair<CallBase *, unsigned>, 16> Clears;
    for (BasicBlock *BB : RPOT) {
      BitVector State = In[BB];
      transfer(*BB, State, &Clears);
    }
    Constant *Null = ConstantPointerNull::get(cast<PointerType>(Ptr0Ty));
    for (auto [S, Slot] : Clears) {
      IRBuilder<> Builder(S);
      Builder.CreateStore(Null, getSlotPtr(Builder, Slot));
    }
  }

  FunctionCallee LeaveFrameFunc = getOrCreateLeaveFrame(*Mod);
  auto emitLeaveFrame = [&](Instruction *InsertBefore) {
    IRBuilder<> Builder(InsertBefore);
    withDebugLoc(Builder.CreateCall(
        LeaveFrameFunc,
        {FrameAlloca, ConstantInt::get(Int32Ty, 0),
         ConstantInt::get(Int32Ty, TotalSlots)}));
  };

  if (FrameLeaveMarkers.empty()) {
    for (BasicBlock &BB : F) {
      Instruction *Terminator = BB.getTerminator();
      if (isa<ReturnInst>(Terminator) || isa<ResumeInst>(Terminator))
        emitLeaveFrame(Terminator);
    }
  } else {
    for (CallInst *CI : FrameLeaveMarkers)
      emitLeaveFrame(CI);

    SmallPtrSet<const Instruction *, 4> LeaveMarkerSet(
        FrameLeaveMarkers.begin(), FrameLeaveMarkers.end());
    SmallPtrSet<const BasicBlock *, 16> Visited;
    SmallVector<const Instruction *, 16> Worklist;
    Worklist.push_back(&*F.getEntryBlock().begin());
    Visited.insert(&F.getEntryBlock());
    while (!Worklist.empty()) {
      const Instruction *I = Worklist.pop_back_val();
      for (; I; I = I->getNextNode()) {
        if (LeaveMarkerSet.contains(I))
          break;
        if (!I->isTerminator())
          continue;
        if (isa<ReturnInst>(I) || isa<ResumeInst>(I)) {
          std::string Msg;
          raw_string_ostream OS(Msg);
          OS << "kotlin-build-shadow-stack: exit '" << I->getParent()->getName()
             << "' of function '" << F.getName()
             << "' is reachable without passing a " << FrameLeaveMarkerName
             << " marker";
          report_fatal_error(StringRef(Msg));
        }
        for (const BasicBlock *Succ : successors(I->getParent()))
          if (Visited.insert(Succ).second)
            Worklist.push_back(&*Succ->begin());
        break;
      }
    }
  }

  auto passesStackMemory = [](const CallInst *CI) {
    return llvm::any_of(CI->args(), [](const Use &Arg) {
      return Arg->getType()->isPointerTy() &&
             isa<AllocaInst>(getUnderlyingObject(Arg.get()));
    });
  };
  for (BasicBlock &BB : F) {
    bool FramePopped = false;
    for (Instruction &I : BB) {
      auto *CI = dyn_cast<CallInst>(&I);
      if (!CI)
        continue;
      if (CI->getCalledOperand() == LeaveFrameFunc.getCallee()) {
        FramePopped = true;
        continue;
      }
      if (!FramePopped || passesStackMemory(CI))
        CI->setTailCallKind(CallInst::TCK_None);
    }
  }

  FunctionCallee SetCurrentFrameFunc = getOrCreateSetCurrentFrame(*Mod);
  SmallPtrSet<const Instruction *, 4> SetCurrentMarkerSet;
  for (CallInst *CI : FrameSetCurrentMarkers)
    SetCurrentMarkerSet.insert(CI);

  for (CallInst *CI : FrameSetCurrentMarkers) {
    IRBuilder<> MarkerBuilder(CI);
    withDebugLoc(MarkerBuilder.CreateCall(SetCurrentFrameFunc, {FrameAlloca}));
  }

  auto findKotlinCallBeforeMarker =
      [&](const LandingPadInst *LP) -> const Function * {
    SmallPtrSet<const BasicBlock *, 8> Visited;
    SmallVector<const Instruction *, 8> Worklist;
    Worklist.push_back(LP->getNextNode());
    Visited.insert(LP->getParent());
    while (!Worklist.empty()) {
      const Instruction *I = Worklist.pop_back_val();
      for (; I; I = I->getNextNode()) {
        if (SetCurrentMarkerSet.contains(I))
          break;
        if (const auto *Call = dyn_cast<CallBase>(I)) {
          const Function *Callee = Call->getCalledFunction();
          if (Callee && Callee->hasFnAttribute(KotlinGcFrameAttrName))
            return Callee;
        }
        if (I->isTerminator()) {
          for (const BasicBlock *Succ : successors(I->getParent()))
            if (Visited.insert(Succ).second)
              Worklist.push_back(&*Succ->getFirstNonPHIIt());
          break;
        }
      }
    }
    return nullptr;
  };

  for (BasicBlock &BB : F) {
    auto *LP = BB.getLandingPadInst();
    if (!LP)
      continue;
    if (!LP->isCleanup())
      LP->setCleanup(true);

    if (!DT->isReachableFromEntry(&BB) ||
        !LP->getMetadata(KotlinLandingPadMetadataName))
      continue;

    if (const Function *Callee = findKotlinCallBeforeMarker(LP)) {
      std::string Msg;
      raw_string_ostream OS(Msg);
      OS << "kotlin-build-shadow-stack: landing pad '" << BB.getName()
         << "' of function '" << F.getName() << "' reaches a call to '"
         << Callee->getName() << "' without passing a "
         << FrameSetCurrentMarkerName << " marker";
      report_fatal_error(StringRef(Msg));
    }
  }

  eraseFrameMarkers();
  Bases.eraseUnused();

  lowerAddressSpaces(F);

  return true;
}

static void mergeThreadLocalAddresses(Function &F) {
  SmallVector<IntrinsicInst *, 8> Calls;
  for (Instruction &I : instructions(F)) {
    auto *II = dyn_cast<IntrinsicInst>(&I);
    if (II && II->getIntrinsicID() == Intrinsic::threadlocal_address)
      Calls.push_back(II);
  }
  if (Calls.size() < 2)
    return;
  DominatorTree DT(F);
  SmallPtrSet<IntrinsicInst *, 8> Erased;
  for (IntrinsicInst *Call : Calls) {
    for (IntrinsicInst *Other : Calls) {
      if (Other == Call || Erased.contains(Other) ||
          Other->getArgOperand(0) != Call->getArgOperand(0) ||
          !DT.dominates(Other, Call))
        continue;
      Call->replaceAllUsesWith(Other);
      Call->eraseFromParent();
      Erased.insert(Call);
      break;
    }
  }
}

static void inlineFrameHelpers(Module &M) {
  SmallSetVector<Function *, 32> Callers;
  for (const char *Name : {"EnterFrame", "LeaveFrame", "SetCurrentFrame"}) {
    Function *Helper = M.getFunction(Name);
    if (!Helper || Helper->isDeclaration())
      continue;
    SmallVector<CallBase *, 32> Calls;
    for (User *U : Helper->users()) {
      auto *CB = dyn_cast<CallBase>(U);
      if (CB && CB->getCalledFunction() == Helper &&
          CB->getFunction()->hasFnAttribute(KotlinGcFrameAttrName))
        Calls.push_back(CB);
    }
    for (CallBase *CB : Calls) {
      Function *Caller = CB->getFunction();
      if (DISubprogram *SP = Caller->getSubprogram()) {
        if (!CB->getDebugLoc())
          CB->setDebugLoc(DILocation::get(M.getContext(), 0, 0, SP));
      }
      Callers.insert(Caller);
      InlineFunctionInfo IFI;
      InlineFunction(*CB, IFI);
    }
    if (Helper->hasLocalLinkage() && Helper->use_empty())
      Helper->eraseFromParent();
  }
  for (Function *F : Callers) {
    if (!F->getSubprogram())
      stripDebugInfo(*F);
    mergeThreadLocalAddresses(*F);
  }
}

static bool buildShadowStackOnModule(Module &M) {
  SmallVector<Function *, 32> Definitions;
  for (Function &F : M) {
    if (!F.isDeclaration())
      Definitions.push_back(&F);
  }

  bool Changed = false;
  for (Function *F : Definitions)
    Changed |= buildShadowStack(*F);

  lowerModuleDeclarations(M);

  inlineFrameHelpers(M);

  for (const char *Name :
       {ReturnSlotMarkerName, FrameEnterMarkerName, FrameLeaveMarkerName,
        FrameSetCurrentMarkerName, StackObjectMarkerName,
        KeepAliveMarkerName}) {
    Function *Marker = M.getFunction(Name);
    if (!Marker)
      continue;
    if (!Marker->use_empty()) {
      std::string Msg;
      raw_string_ostream OS(Msg);
      OS << "kotlin-build-shadow-stack: " << Marker->getNumUses()
         << " unlowered call(s) to " << Name << " remain";
      if (auto *I = dyn_cast<Instruction>(*Marker->user_begin()))
        OS << ", first in function '" << I->getFunction()->getName() << "'";
      report_fatal_error(StringRef(Msg));
    }
    Marker->eraseFromParent();
  }
  return Changed;
}

extern "C" void LLVMKotlinBuildShadowStack(LLVMModuleRef M) {
  buildShadowStackOnModule(*unwrap(M));
}

PreservedAnalyses BuildShadowStackPass::run(Module &M, ModuleAnalysisManager &) {
  buildShadowStackOnModule(M);
  return PreservedAnalyses::none();
}
