// Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language
// contributors. Use of this source code is governed by the Apache 2.0 license
// that can be found in the license/LICENSE.txt file.

#include "BuildShadowStack.h"

#include "llvm-c/Core.h"
#include "llvm/ADT/BitVector.h"
#include "llvm/ADT/DenseMap.h"
#include "llvm/ADT/DepthFirstIterator.h"
#include "llvm/ADT/PostOrderIterator.h"
#include "llvm/ADT/STLExtras.h"
#include "llvm/ADT/SetVector.h"
#include "llvm/ADT/SmallPtrSet.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringRef.h"
#include "llvm/Analysis/ValueTracking.h"
#include "llvm/Analysis/VectorUtils.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/DataLayout.h"
#include "llvm/IR/DebugInfo.h"
#include "llvm/IR/DebugInfoMetadata.h"
#include "llvm/IR/DebugLoc.h"
#include "llvm/IR/DerivedTypes.h"
#include "llvm/IR/Dominators.h"
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

using namespace llvm;
using namespace llvm::kotlin;

static constexpr unsigned GCAddressSpace = BuildShadowStackPass::GCAddressSpace;

static constexpr const char *ReturnSlotMarkerName = "Kotlin_gc_returnSlot";
static constexpr const char *FrameEnterMarkerName = "Kotlin_gc_frameEnter";
static constexpr const char *FrameLeaveMarkerName = "Kotlin_gc_frameLeave";
static constexpr const char *FrameSetCurrentMarkerName =
    "Kotlin_gc_frameSetCurrent";
static constexpr const char *StackObjectMarkerName = "Kotlin_gc_stackObject";
static constexpr const char *KeepAliveMarkerName = "Kotlin_gc_keepAlive";

static constexpr const char *MarkerNames[] = {
    ReturnSlotMarkerName,  FrameEnterMarkerName,      FrameLeaveMarkerName,
    FrameSetCurrentMarkerName, StackObjectMarkerName, KeepAliveMarkerName,
};

static constexpr const char *KotlinLandingPadMetadataName =
    "kotlin.gc.landingpad";
static constexpr const char *KotlinCalleeExitMetadataName =
    "kotlin.gc.callee_exit";
static constexpr const char *KotlinGcFrameAttrName = "kotlin-gc-frame";

[[noreturn]] static void fail(const Twine &Msg) {
  report_fatal_error("kotlin-build-shadow-stack: " + Msg);
}

struct ValueTypeAccess : GlobalValue {
  static constexpr Type *GlobalValue::*Field = &ValueTypeAccess::ValueType;
};

static void setFunctionType(Function &F, FunctionType *FTy) {
  F.*ValueTypeAccess::Field = FTy;
}

static unsigned getFrameOverlaySlots(const Module &M) {
  const DataLayout &DL = M.getDataLayout();
  LLVMContext &Ctx = M.getContext();

  Type *PtrTy = PointerType::getUnqual(Ctx);
  Type *I32Ty = Type::getInt32Ty(Ctx);
  StructType *FO = StructType::get(Ctx, {PtrTy, I32Ty, I32Ty});
  return static_cast<unsigned>(DL.getTypeStoreSize(FO) / DL.getPointerSize());
}

static FunctionCallee getOrCreateFrameFunc(Module &M, StringRef Name) {
  LLVMContext &Ctx = M.getContext();
  Type *I32Ty = Type::getInt32Ty(Ctx);
  return M.getOrInsertFunction(
      Name, FunctionType::get(Type::getVoidTy(Ctx),
                              {PointerType::getUnqual(Ctx), I32Ty, I32Ty},
                              false));
}

static FunctionCallee getOrCreateSetCurrentFrame(Module &M) {
  LLVMContext &Ctx = M.getContext();
  FunctionType *FTy = FunctionType::get(Type::getVoidTy(Ctx),
                                        {PointerType::getUnqual(Ctx)}, false);
  return M.getOrInsertFunction("SetCurrentFrame", FTy);
}

static bool isInteriorPointerFunction(const Function &F) {
  StringRef Name = F.getName();
  return Name == "Kotlin_arrayGetElementAddress" ||
         Name == "Kotlin_intArrayGetElementAddress" ||
         Name == "Kotlin_longArrayGetElementAddress";
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

  if (Name == "Kotlin_Any_getTypeInfo" || Name == "__cxa_begin_catch" ||
      Name == "__cxa_end_catch")
    return true;

  if (Name.starts_with("Kotlin_mm_switchThreadStateNative"))
    return true;

  return llvm::is_contained(MarkerNames, Name);
}

static void collectCalleeExitMarkers(Function &F,
                                     SmallVectorImpl<CallInst *> &Out) {
  for (Instruction &I : instructions(F)) {
    auto *CI = dyn_cast<CallInst>(&I);
    if (CI && CI->getIntrinsicID() == Intrinsic::sideeffect &&
        CI->getMetadata(KotlinCalleeExitMetadataName))
      Out.push_back(CI);
  }
}

static bool isSafepointOrAllocationCall(const Instruction &I,
                                        bool InNativeState) {
  auto *Call = dyn_cast<CallBase>(&I);
  if (!Call)
    return false;

  if (Call->isInlineAsm())
    return false;

  if (Call->getIntrinsicID() != Intrinsic::not_intrinsic)
    return false;

  // A call that can neither throw nor write memory cannot reach a safepoint,
  // but in the native thread state the GC scans the frame concurrently.
  if (!InNativeState && Call->doesNotThrow() &&
      (Call->doesNotAccessMemory() || Call->onlyReadsMemory()))
    return false;

  const Function *Callee = Call->getCalledFunction();
  if (Callee && isNonSafepointFunction(*Callee))
    return false;

  return true;
}

static bool isCallTo(const Instruction &I, StringRef NamePrefix) {
  auto *Call = dyn_cast<CallBase>(&I);
  const Function *Callee = Call ? Call->getCalledFunction() : nullptr;
  return Callee && Callee->getName().starts_with(NamePrefix);
}

/// Collects the reachable calls at which the GC may scan the frame, tracking
/// which calls may run between a switch to the native thread state and the
/// switch back.
static SmallVector<CallBase *, 16> collectSafepoints(Function &F,
                                                     const DominatorTree &DT) {
  DenseMap<const BasicBlock *, bool> NativeOut;
  auto walk = [&](BasicBlock &BB, SmallVectorImpl<CallBase *> *Safepoints) {
    bool InNative = llvm::any_of(predecessors(&BB), [&](BasicBlock *Pred) {
      return NativeOut.lookup(Pred);
    });
    for (Instruction &I : BB) {
      if (Safepoints && isSafepointOrAllocationCall(I, InNative))
        Safepoints->push_back(cast<CallBase>(&I));
      if (isCallTo(I, "Kotlin_mm_switchThreadStateNative"))
        InNative = true;
      else if (isCallTo(I, "Kotlin_mm_switchThreadStateRunnable"))
        InNative = false;
    }
    return InNative;
  };
  ReversePostOrderTraversal<Function *> RPOT(&F);
  for (bool Changed = true; Changed;) {
    Changed = false;
    for (BasicBlock *BB : RPOT) {
      bool Out = walk(*BB, nullptr);
      if (Out != NativeOut.lookup(BB)) {
        NativeOut[BB] = Out;
        Changed = true;
      }
    }
  }
  SmallVector<CallBase *, 16> Safepoints;
  for (BasicBlock &BB : F) {
    if (DT.isReachableFromEntry(&BB))
      walk(BB, &Safepoints);
  }
  return Safepoints;
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

static bool containsPointerType(Type *Ty) {
  if (Ty->isPtrOrPtrVectorTy())
    return true;
  for (Type *Sub : Ty->subtypes()) {
    if (containsPointerType(Sub))
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

static Value *stripOffsets(Value *V) {
  while (true) {
    if (auto *GEP = dyn_cast<GetElementPtrInst>(V)) {
      V = GEP->getPointerOperand();
      continue;
    }
    if (auto *FI = dyn_cast<FreezeInst>(V)) {
      V = FI->getOperand(0);
      continue;
    }
    break;
  }
  return V;
}

template <typename Pred>
static bool tracesTo(Value *V, Pred &&Match, bool FollowCasts = false) {
  SmallPtrSet<Value *, 8> Visited;
  SmallVector<Value *, 8> Worklist = {V};
  while (!Worklist.empty()) {
    Value *Curr = Worklist.pop_back_val();
    if (!Visited.insert(Curr).second)
      continue;
    if (Match(Curr))
      return true;
    if (auto *GEP = dyn_cast<GetElementPtrInst>(Curr))
      Worklist.push_back(GEP->getPointerOperand());
    else if (auto *FI = dyn_cast<FreezeInst>(Curr))
      Worklist.push_back(FI->getOperand(0));
    else if (auto *ASC = dyn_cast<AddrSpaceCastInst>(Curr);
             ASC && FollowCasts)
      Worklist.push_back(ASC->getPointerOperand());
    else if (auto *Phi = dyn_cast<PHINode>(Curr))
      append_range(Worklist, Phi->incoming_values());
    else if (auto *Sel = dyn_cast<SelectInst>(Curr)) {
      Worklist.push_back(Sel->getTrueValue());
      Worklist.push_back(Sel->getFalseValue());
    }
  }
  return false;
}

static bool tracesToGCReference(Value *V) {
  return tracesTo(
      V,
      [](Value *Curr) {
        if (isInteriorPointerCall(Curr))
          return true;
        if (auto *ASC = dyn_cast<AddrSpaceCastInst>(Curr))
          return isGCReferenceType(ASC->getSrcTy());
        if (auto *GEP = dyn_cast<GetElementPtrInst>(Curr))
          return isGCReferenceType(GEP->getPointerOperandType());
        if (auto *FI = dyn_cast<FreezeInst>(Curr))
          return isGCReferenceType(FI->getType());
        return false;
      },
      /*FollowCasts=*/true);
}

static bool isStackOrStaticObject(Value *V) {
  SmallPtrSet<Value *, 8> Visited;
  SmallVector<Value *, 8> Worklist = {V};
  bool FoundObject = false;
  while (!Worklist.empty()) {
    Value *Curr = getUnderlyingObject(stripOffsets(Worklist.pop_back_val()));
    if (!Visited.insert(Curr).second)
      continue;
    if (isa<AllocaInst>(Curr) || isa<GlobalValue>(Curr)) {
      FoundObject = true;
      continue;
    }
    if (isa<ConstantData>(Curr))
      continue;
    if (auto *Phi = dyn_cast<PHINode>(Curr))
      append_range(Worklist, Phi->incoming_values());
    else if (auto *Sel = dyn_cast<SelectInst>(Curr)) {
      Worklist.push_back(Sel->getTrueValue());
      Worklist.push_back(Sel->getFalseValue());
    } else {
      return false;
    }
  }
  return FoundObject;
}

static bool isAggregateOrVectorOp(const Value *V) {
  return isa<InsertValueInst, ExtractValueInst, InsertElementInst,
             ShuffleVectorInst, ExtractElementInst>(V);
}

static bool isDerivedPointer(Value *V, SmallPtrSetImpl<Value *> &Visited) {
  if (!V->getType()->isPtrOrPtrVectorTy() && !V->getType()->isAggregateType())
    return false;
  if (!Visited.insert(V).second)
    return false;

  if (isa<GetElementPtrInst>(V) || isa<AllocaInst>(V) || isa<FreezeInst>(V) ||
      isInteriorPointerCall(V))
    return true;

  if (auto *CE = dyn_cast<ConstantExpr>(V)) {
    return CE->getOpcode() == Instruction::GetElementPtr ||
           CE->getOpcode() == Instruction::IntToPtr;
  }

  if (auto *ITP = dyn_cast<IntToPtrInst>(V))
    return !isGCReferenceType(ITP->getType());

  if (auto *ASC = dyn_cast<AddrSpaceCastInst>(V)) {
    return isGCReferenceType(ASC->getSrcTy()) ||
           isStackOrStaticObject(ASC->getPointerOperand()) ||
           tracesToGCReference(ASC->getPointerOperand());
  }

  if (isAggregateOrVectorOp(V) || isa<PHINode, SelectInst>(V)) {
    return llvm::any_of(cast<Instruction>(V)->operand_values(), [&](Value *Op) {
      return isDerivedPointer(Op, Visited);
    });
  }

  return false;
}

static bool isDerivedPointer(Value *V) {
  SmallPtrSet<Value *, 8> Visited;
  return isDerivedPointer(V, Visited);
}

static bool isCandidateRoot(Value *V) {
  return isGCReferenceType(V->getType()) && !isa<Constant>(V) &&
         !isDerivedPointer(V);
}

using PlainRootsMap = DenseMap<Value *, SmallVector<Instruction *, 1>>;

/// Collects the references an aggregate holds, extracting those that are not
/// already available as scalars before the builder's insertion point. Returns
/// false for a shape it cannot take apart.
static bool extractReferences(IRBuilder<> &B, Value *Agg,
                              SmallVectorImpl<Value *> &Refs) {
  Type *Ty = Agg->getType();
  if (isGCReferenceType(Ty)) {
    Refs.push_back(Agg);
    return true;
  }
  if (auto *VT = dyn_cast<FixedVectorType>(Ty)) {
    for (unsigned I = 0; I < VT->getNumElements(); ++I) {
      Value *Elt = findScalarElement(Agg, I);
      if (!Elt)
        Elt = B.CreateExtractElement(Agg, I, Agg->getName() + ".ref");
      Refs.push_back(Elt);
    }
    return true;
  }
  if (!isa<StructType, ArrayType>(Ty))
    return false;
  bool IsStruct = isa<StructType>(Ty);
  unsigned NumElements =
      IsStruct ? Ty->getStructNumElements() : Ty->getArrayNumElements();
  for (unsigned I = 0; I < NumElements; ++I) {
    Type *EltTy =
        IsStruct ? Ty->getStructElementType(I) : Ty->getArrayElementType();
    if (!containsGCReferenceType(EltTy))
      continue;
    Value *Elt = FindInsertedValue(Agg, {I});
    if (!Elt)
      Elt = B.CreateExtractValue(Agg, {I}, Agg->getName() + ".ref");
    if (!extractReferences(B, Elt, Refs))
      return false;
  }
  return true;
}

static Instruction *getFirstNonAlloca(BasicBlock &BB) {
  BasicBlock::iterator It = BB.begin();
  while (It != BB.end() && isa<AllocaInst>(*It))
    ++It;
  return &*It;
}

static Instruction *getStorePoint(Value *V) {
  if (auto *Arg = dyn_cast<Argument>(V))
    return getFirstNonAlloca(Arg->getParent()->getEntryBlock());
  if (auto *Phi = dyn_cast<PHINode>(V))
    return &*Phi->getParent()->getFirstInsertionPt();
  if (auto *II = dyn_cast<InvokeInst>(V)) {
    BasicBlock *Normal = II->getNormalDest();
    return Normal->getSinglePredecessor() ? &*Normal->getFirstInsertionPt()
                                          : nullptr;
  }
  if (auto *Inst = dyn_cast<Instruction>(V))
    return Inst->getNextNode();
  return nullptr;
}

static void splitRootInvokeEdges(Function &F,
                                 const SmallSetVector<Value *, 8> &PlainRoots,
                                 DominatorTree &DT) {
  SmallVector<InvokeInst *, 8> Invokes;
  for (Instruction &I : instructions(F)) {
    auto *II = dyn_cast<InvokeInst>(&I);
    if (II && (containsGCReferenceType(II->getType()) || PlainRoots.contains(II)))
      Invokes.push_back(II);
  }
  for (InvokeInst *II : Invokes) {
    BasicBlock *Normal = II->getNormalDest();
    if (!Normal->getSinglePredecessor())
      SplitEdge(II->getParent(), Normal, &DT);
  }
}

class MergeBases {
public:
  explicit MergeBases(Function &F, const DominatorTree &DT,
                      const SmallSetVector<Value *, 8> &PlainRoots,
                      const PlainRootsMap &PlainRootsByBase)
      : GCNull(ConstantPointerNull::get(
            PointerType::get(F.getContext(), GCAddressSpace))),
        DT(DT), PlainRoots(PlainRoots), PlainRootsByBase(PlainRootsByBase) {
    SmallVector<Instruction *, 16> Merges;
    for (BasicBlock &BB : F) {
      if (!DT.isReachableFromEntry(&BB))
        continue;
      for (Instruction &I : BB) {
        if (!isa<PHINode>(I) && !isa<SelectInst>(I))
          continue;
        if (!I.getType()->isPointerTy() || isCandidateRoot(&I) ||
            PlainRoots.contains(&I))
          continue;
        if (isGCReferenceType(I.getType()) || tracesToGCReference(&I) ||
            tracesTo(&I, [&](Value *V) { return isLaunderedPlainObject(V); }))
          Merges.push_back(&I);
      }
    }
    for (Instruction *M : Merges) {
      if (auto *BaseInst = dyn_cast_or_null<Instruction>(findBase(M))) {
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

  Instruction *findPlainRoot(Value *V, const Instruction *At) const {
    if (PlainRoots.contains(V))
      return cast<Instruction>(V);
    auto It = PlainRootsByBase.find(stripOffsets(V));
    if (It == PlainRootsByBase.end())
      return nullptr;
    for (Instruction *Root : It->second) {
      if (!At || DT.dominates(Root, At))
        return Root;
    }
    return nullptr;
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
    if (Instruction *Root = findPlainRoot(I, I))
      return findBase(Root);
    if (auto *GEP = dyn_cast<GetElementPtrInst>(I))
      return findBase(GEP->getPointerOperand());
    if (auto *FI = dyn_cast<FreezeInst>(I))
      return findBase(FI->getOperand(0));
    if (auto *ASC = dyn_cast<AddrSpaceCastInst>(I))
      return findBase(ASC->getPointerOperand());
    if (isInteriorPointerCall(I))
      return findBase(cast<CallBase>(I)->getArgOperand(0));
    if (isa<PHINode, SelectInst>(I))
      return mergeBases(I);
    return GCNull;
  }

  Value *launderedBase(Value *X) {
    Instruction *InsertPt = getStorePoint(X);
    if (!InsertPt)
      return GCNull;
    auto *Cast =
        new AddrSpaceCastInst(X, GCNull->getType(), X->getName() + ".base",
                              InsertPt->getIterator());
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

  bool isLaunderedPlainObject(Value *V) const {
    return PlainRootsByBase.contains(stripOffsets(V)) ||
           ((isa<Instruction>(V) || isa<Argument>(V)) &&
            llvm::any_of(V->users(), [](User *U) {
              return isa<AddrSpaceCastInst>(U) && isCandidateRoot(U);
            }));
  }

  Value *findBaseAt(Value *V, Instruction *At) {
    if (Instruction *Root = findPlainRoot(V, At))
      return findBase(Root);
    Value *X = stripOffsets(V);
    if (!isLaunderedPlainObject(X))
      return findBase(V);
    for (User *U : X->users()) {
      auto *ASC = dyn_cast<AddrSpaceCastInst>(U);
      if (ASC && isCandidateRoot(ASC) &&
          DT.isReachableFromEntry(ASC->getParent()) && DT.dominates(ASC, At))
        return ASC;
    }
    return GCNull;
  }

  Value *mergeBases(Instruction *M) {
    auto *Phi = dyn_cast<PHINode>(M);
    Instruction *Placeholder =
        Phi ? static_cast<Instruction *>(
                  PHINode::Create(GCNull->getType(), Phi->getNumIncomingValues(),
                                  M->getName() + ".base", M->getIterator()))
            : SelectInst::Create(cast<SelectInst>(M)->getCondition(), GCNull,
                                 GCNull, M->getName() + ".base",
                                 M->getIterator());
    Cache[M] = Placeholder;

    SmallVector<Value *, 4> Bases;
    if (Phi) {
      for (unsigned I = 0; I < Phi->getNumIncomingValues(); ++I)
        Bases.push_back(findBaseAt(Phi->getIncomingValue(I),
                                   Phi->getIncomingBlock(I)->getTerminator()));
    } else {
      auto *Sel = cast<SelectInst>(M);
      Bases.push_back(findBaseAt(Sel->getTrueValue(), Sel));
      Bases.push_back(findBaseAt(Sel->getFalseValue(), Sel));
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

    if (Phi) {
      auto *BasePhi = cast<PHINode>(Placeholder);
      for (unsigned I = 0; I < Phi->getNumIncomingValues(); ++I)
        BasePhi->addIncoming(Bases[I], Phi->getIncomingBlock(I));
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
  const PlainRootsMap &PlainRootsByBase;
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
      } else if (HasNonDominatingPlainUses) {
        if (!ReachableFromDef.contains(SBB))
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
    BasicBlock *BaseDefBB = nullptr;
    if (auto *BaseInst = dyn_cast<Instruction>(Base))
      BaseDefBB = BaseInst->getParent();
    if (!IsPlainRoot && DefBB && DefBB != BaseDefBB) {
      HasNonDominatingPlainUses = true;
      SmallVector<BasicBlock *, 16> BBWorklist;
      ReachableFromDef.insert(DefBB);
      BBWorklist.push_back(DefBB);
      while (!BBWorklist.empty()) {
        BasicBlock *BB = BBWorklist.pop_back_val();
        for (BasicBlock *Succ : successors(BB)) {
          if (Succ != BaseDefBB && DT.isReachableFromEntry(Succ) &&
              ReachableFromDef.insert(Succ).second)
            BBWorklist.push_back(Succ);
        }
      }
    }
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
        if (isDerivedPointer(UI) ||
            (!isGCReferenceType(UI->getType()) &&
             ((isAggregateOrVectorOp(UI) && containsPointerType(UI->getType())) ||
              isa<PHINode, SelectInst>(UI)))) {
          if (Visited.insert(UI).second)
            Worklist.push_back(UI);
          continue;
        }
        if (DominatingDef) {
          if (UI == V || isCandidateRoot(UI))
            continue;
          if (!DT.dominates(DominatingDef, U)) {
            if (!HasNonDominatingPlainUses)
              continue;
            if (auto *Phi = dyn_cast<PHINode>(UI)) {
              if (!ReachableFromDef.contains(Phi->getIncomingBlock(U)))
                continue;
            } else {
              if (UI->getParent() == DefBB ||
                  !ReachableFromDef.contains(UI->getParent()))
                continue;
            }
          }
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
      if (BB == DefBB || !DT.isReachableFromEntry(BB))
        return;
      if (HasNonDominatingPlainUses && !ReachableFromDef.contains(BB))
        return;
      if (LiveIn.insert(BB).second)
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
  bool HasNonDominatingPlainUses = false;
  BasicBlock *DefBB = nullptr;
  SmallPtrSet<BasicBlock *, 16> ReachableFromDef;
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
  OS << "unhandled addrspace(" << GCAddressSpace << ") constant: " << *C;
  fail(Msg);
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
  NewDecl->copyMetadata(&Decl, 0);
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
  for (Instruction &I : instructions(F)) {
    if (auto *ASC = dyn_cast<AddrSpaceCastInst>(&I)) {
      if (isGCReferenceType(ASC->getSrcTy()) ||
          isGCReferenceType(ASC->getDestTy()))
        GCCasts.push_back(ASC);
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

  for (Instruction &I : instructions(F)) {
    if (containsGCReferenceType(I.getType()))
      I.mutateType(lowerType(I.getType()));
    for (Use &Op : I.operands()) {
      if (auto *C = dyn_cast<Constant>(Op.get())) {
        if (!isa<GlobalValue>(C) && constantMentionsGC(C))
          Op.set(lowerConstant(C));
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

  for (Instruction &I : instructions(F)) {
    if (auto *AI = dyn_cast<AllocaInst>(&I)) {
      AI->setAllocatedType(lowerType(AI->getAllocatedType()));
    } else if (auto *GEP = dyn_cast<GetElementPtrInst>(&I)) {
      GEP->setSourceElementType(lowerType(GEP->getSourceElementType()));
      SmallVector<Value *, 4> Indices(GEP->indices());
      GEP->setResultElementType(GetElementPtrInst::getIndexedType(
          GEP->getSourceElementType(), Indices));
    } else if (auto *CB = dyn_cast<CallBase>(&I)) {
      FunctionType *CFTy = CB->getFunctionType();
      FunctionType *LoweredCFTy = cast<FunctionType>(lowerType(CFTy));
      if (LoweredCFTy != CFTy)
        CB->mutateFunctionType(LoweredCFTy);
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

template <typename MatchPred, typename StopPred>
static const Instruction *findReachableInst(const Instruction *Start,
                                            MatchPred &&Match,
                                            StopPred &&Stop) {
  SmallPtrSet<const BasicBlock *, 16> Visited;
  SmallVector<const Instruction *, 16> Worklist = {Start};
  while (!Worklist.empty()) {
    for (const Instruction *I = Worklist.pop_back_val(); I;
         I = I->getNextNode()) {
      if (Stop(I))
        break;
      if (Match(I))
        return I;
      if (I->isTerminator()) {
        for (const BasicBlock *Succ : successors(I->getParent())) {
          if (Visited.insert(Succ).second)
            Worklist.push_back(&Succ->front());
        }
      }
    }
  }
  return nullptr;
}

/// Assigns frame slots to the live roots. A root is stored to its slot once,
/// at its store point, and the slot is read only by the GC at the safepoints
/// the root is live across. So the slot of V is live at a point if V's store
/// may have run before it, and a path from it reaches such a safepoint without
/// passing V's store again. Two roots interfere if one is stored where the
/// slot of the other is live. One forward and one backward bit-vector dataflow
/// over all roots compute this.
///
/// Slots are then assigned greedily in dominator-tree order of the store
/// points. Where every store dominates the safepoints of its root, the
/// interference graph is chordal and this order uses the fewest slots.
static unsigned assignRootSlots(
    Function &F, ArrayRef<Value *> LiveRoots,
    const DenseMap<Value *, SmallVector<CallBase *, 8>> &SafepointsForRoot,
    const DominatorTree &DT, DenseMap<Value *, unsigned> &SlotAssignment) {
  unsigned N = LiveRoots.size();
  if (N == 0)
    return 0;

  DenseMap<const Instruction *, BitVector> LiveAcross;
  DenseMap<const Instruction *, SmallVector<unsigned, 1>> StoredAt;
  SmallVector<Instruction *, 16> StorePoints;
  for (unsigned R = 0; R < N; ++R) {
    for (CallBase *S : SafepointsForRoot.find(LiveRoots[R])->second) {
      BitVector &Roots = LiveAcross[S];
      Roots.resize(N);
      Roots.set(R);
    }
    StorePoints.push_back(getStorePoint(LiveRoots[R]));
    if (StorePoints.back())
      StoredAt[StorePoints.back()].push_back(R);
  }

  SmallVector<BitVector, 16> Interferes(N, BitVector(N));
  auto addInterference = [&](unsigned R, const BitVector &Others) {
    Interferes[R] |= Others;
    for (unsigned U : Others.set_bits())
      Interferes[U].set(R);
  };

  // Which roots may have been stored, after the stores at each store point.
  ReversePostOrderTraversal<Function *> RPOT(&F);
  DenseMap<const BasicBlock *, BitVector> StoredOut;
  DenseMap<const Instruction *, BitVector> StoredAfter;
  for (bool Changed = true; Changed;) {
    Changed = false;
    for (BasicBlock *BB : RPOT) {
      BitVector Stored(N);
      for (BasicBlock *Pred : predecessors(BB)) {
        if (auto It = StoredOut.find(Pred); It != StoredOut.end())
          Stored |= It->second;
      }
      for (Instruction &I : *BB) {
        if (auto It = StoredAt.find(&I); It != StoredAt.end()) {
          for (unsigned R : It->second)
            Stored.set(R);
          StoredAfter[&I] = Stored;
        }
      }
      if (BitVector &Out = StoredOut[BB]; Out != Stored) {
        Out = std::move(Stored);
        Changed = true;
      }
    }
  }

  // Which slots are live, walking backward; at a store point, before the
  // stores there kill their own roots, every live and stored slot interferes.
  DenseMap<const BasicBlock *, BitVector> LiveIn;
  auto walkBackward = [&](BasicBlock &BB, bool Record) {
    BitVector Live(N);
    for (BasicBlock *Succ : successors(&BB))
      Live |= LiveIn.lookup(Succ);
    for (Instruction &I : reverse(BB)) {
      if (auto It = LiveAcross.find(&I); It != LiveAcross.end())
        Live |= It->second;
      auto It = StoredAt.find(&I);
      if (It == StoredAt.end())
        continue;
      if (Record) {
        BitVector Clobbered = Live;
        Clobbered &= StoredAfter.lookup(&I);
        for (unsigned R : It->second)
          addInterference(R, Clobbered);
      }
      for (unsigned R : It->second)
        Live.reset(R);
    }
    return Live;
  };
  for (bool Changed = true; Changed;) {
    Changed = false;
    for (BasicBlock *BB : reverse(RPOT)) {
      BitVector Live = walkBackward(*BB, /*Record=*/false);
      if (BitVector &In = LiveIn[BB]; In != Live) {
        In = std::move(Live);
        Changed = true;
      }
    }
  }
  for (BasicBlock *BB : RPOT)
    walkBackward(*BB, /*Record=*/true);

  // Order the roots by their store points in dominator-tree preorder.
  SmallVector<unsigned, 16> Order;
  BitVector Ordered(N);
  for (const DomTreeNode *Node : depth_first(DT.getRootNode())) {
    for (Instruction &I : *Node->getBlock()) {
      if (auto It = StoredAt.find(&I); It != StoredAt.end()) {
        for (unsigned R : It->second) {
          Order.push_back(R);
          Ordered.set(R);
        }
      }
    }
  }

  BitVector All(N, true);
  for (unsigned R = 0; R < N; ++R) {
    if (!Ordered.test(R)) {
      // Without a reachable store point, the slot gets no dataflow facts.
      addInterference(R, All);
      Order.push_back(R);
      continue;
    }
    // The dataflow only sees overwrites after a root was stored. Two roots
    // live at one safepoint conflict even when neither store is on the path
    // to the other, which needs a store that does not dominate its safepoint.
    Instruction *Store = StorePoints[R];
    for (CallBase *S : SafepointsForRoot.find(LiveRoots[R])->second) {
      if (S != Store && !DT.dominates(Store, S))
        addInterference(R, LiveAcross[S]);
    }
  }

  // Roots that store the same pointer can share a slot.
  DenseMap<Value *, SmallVector<unsigned, 2>> ByStoredValue;
  for (unsigned R = 0; R < N; ++R) {
    Value *V = LiveRoots[R];
    if (auto *ASC = dyn_cast<AddrSpaceCastInst>(V))
      V = ASC->getPointerOperand();
    ByStoredValue[V].push_back(R);
  }
  for (auto &[V, Roots] : ByStoredValue) {
    for (unsigned R : Roots) {
      for (unsigned U : Roots)
        Interferes[R].reset(U);
    }
  }

  unsigned NumSlots = 0;
  SmallVector<unsigned, 16> SlotOf(N);
  BitVector Colored(N);
  for (unsigned R : Order) {
    BitVector Used(NumSlots + 1);
    for (unsigned U : Interferes[R].set_bits()) {
      if (Colored.test(U))
        Used.set(SlotOf[U]);
    }
    unsigned Slot = Used.find_first_unset();
    SlotOf[R] = Slot;
    Colored.set(R);
    NumSlots = std::max(NumSlots, Slot + 1);
    SlotAssignment[LiveRoots[R]] = Slot;
  }
  return NumSlots;
}

template <typename GetSlotPtrFn>
static void emitSlotClears(
    Function &F, ArrayRef<CallBase *> Safepoints, ArrayRef<CallBase *> ClearPoints,
    ArrayRef<Value *> LiveRoots,
    const DenseMap<Value *, SmallVector<CallBase *, 8>> &ClearPointsForRoot,
    const DenseMap<Value *, unsigned> &SlotAssignment,
    const DenseMap<Instruction *, unsigned> &SlotWrites,
    unsigned NumTrackedSlots, GetSlotPtrFn &&GetSlotPtr) {
  SmallPtrSet<Instruction *, 16> IsSafepoint(Safepoints.begin(),
                                             Safepoints.end());
  DenseMap<Instruction *, BitVector> Clearable;
  for (CallBase *P : ClearPoints)
    Clearable[P] = BitVector(NumTrackedSlots);

  ReversePostOrderTraversal<Function *> RPOT(&F);
  DenseMap<BasicBlock *, BitVector> ScannedIn;
  for (BasicBlock *BB : RPOT)
    ScannedIn[BB] = BitVector(NumTrackedSlots);
  for (bool Changed = true; Changed;) {
    Changed = false;
    for (BasicBlock *BB : reverse(RPOT)) {
      BitVector Scanned(NumTrackedSlots);
      for (BasicBlock *Succ : successors(BB))
        Scanned |= ScannedIn[Succ];
      for (Instruction &I : reverse(*BB)) {
        if (auto It = SlotWrites.find(&I); It != SlotWrites.end())
          Scanned.reset(It->second);
        if (IsSafepoint.contains(&I))
          Scanned.set();
        if (auto It = Clearable.find(&I); It != Clearable.end())
          It->second = Scanned;
      }
      if (Scanned != ScannedIn[BB]) {
        ScannedIn[BB] = std::move(Scanned);
        Changed = true;
      }
    }
  }
  for (Value *V : LiveRoots) {
    if (auto It = ClearPointsForRoot.find(V); It != ClearPointsForRoot.end()) {
      unsigned Slot = SlotAssignment.lookup(V);
      for (CallBase *P : It->second)
        Clearable[P].reset(Slot);
    }
  }

  auto transfer =
      [&](BasicBlock &BB, BitVector &Filled,
          SmallVectorImpl<std::pair<Instruction *, unsigned>> *Clears) {
        for (Instruction &I : BB) {
          if (auto It = Clearable.find(&I); It != Clearable.end()) {
            for (unsigned Slot : It->second.set_bits()) {
              if (!Filled.test(Slot))
                continue;
              Filled.reset(Slot);
              if (Clears)
                Clears->push_back({&I, Slot});
            }
          }
          if (auto It = SlotWrites.find(&I); It != SlotWrites.end())
            Filled.set(It->second);
        }
      };

  DenseMap<BasicBlock *, BitVector> In, Out;
  for (BasicBlock *BB : RPOT) {
    In[BB] = BitVector(NumTrackedSlots);
    Out[BB] = BitVector(NumTrackedSlots);
  }
  for (bool Changed = true; Changed;) {
    Changed = false;
    for (BasicBlock *BB : RPOT) {
      BitVector State(NumTrackedSlots);
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

  SmallVector<std::pair<Instruction *, unsigned>, 16> Clears;
  for (BasicBlock *BB : RPOT) {
    BitVector State = In[BB];
    transfer(*BB, State, &Clears);
  }
  Constant *Null =
      ConstantPointerNull::get(PointerType::getUnqual(F.getContext()));
  for (auto [P, Slot] : Clears) {
    IRBuilder<> Builder(P);
    Builder.CreateStore(Null, GetSlotPtr(Builder, Slot));
  }
}

static bool hasGCAddressSpaceValue(const Function &F) {
  if (containsGCReferenceType(F.getFunctionType()))
    return true;
  for (const Instruction &I : instructions(F)) {
    if (containsGCReferenceType(I.getType()))
      return true;
    for (const Value *Op : I.operand_values()) {
      if (containsGCReferenceType(Op->getType()))
        return true;
      if (auto *C = dyn_cast<Constant>(Op); C && constantMentionsGC(C))
        return true;
    }
  }
  return false;
}

static bool buildShadowStack(Function &F, DominatorTree *DT = nullptr) {
  if (F.isDeclaration() || F.empty())
    return false;

  SmallVector<CallInst *, 4> CalleeExitMarkers;
  collectCalleeExitMarkers(F, CalleeExitMarkers);

  if (!F.hasFnAttribute(KotlinGcFrameAttrName)) {
    if (hasGCAddressSpaceValue(F))
      fail("function '" + F.getName() + "' holds addrspace(" +
           Twine(GCAddressSpace) + ") values but is not marked '" +
           KotlinGcFrameAttrName + "'");
    for (CallInst *CI : CalleeExitMarkers)
      CI->eraseFromParent();
    lowerAddressSpaces(F);
    return false;
  }

  for (BasicBlock &BB : F) {
    if (BB.isEHPad() && !BB.isLandingPad())
      fail("funclet-based exception handling is not supported, in function '" +
           F.getName() + "'");
  }

  std::unique_ptr<DominatorTree> LocalDT;
  if (!DT) {
    LocalDT = std::make_unique<DominatorTree>(F);
    DT = LocalDT.get();
  }

  SmallVector<AllocaInst *, 16> PromotableAllocas;
  for (Instruction &I : F.getEntryBlock()) {
    if (auto *AI = dyn_cast<AllocaInst>(&I); AI && isAllocaPromotable(AI))
      PromotableAllocas.push_back(AI);
  }
  if (!PromotableAllocas.empty())
    PromoteMemToReg(PromotableAllocas, *DT);

  for (Instruction &I : instructions(F)) {
    auto *AI = dyn_cast<AllocaInst>(&I);
    if (!AI)
      continue;
    Type *Ty = AI->getAllocatedType();
    while (auto *AT = dyn_cast<ArrayType>(Ty))
      Ty = AT->getElementType();
    if (isGCReferenceType(Ty))
      fail("alloca '" + AI->getName() + "' in function '" + F.getName() +
           "' holds references but cannot be promoted");
  }

  SmallVector<CallBase *, 16> Safepoints = collectSafepoints(F, *DT);

  SmallVector<CallInst *, 4> ReturnSlotMarkers;
  SmallVector<CallInst *, 1> FrameEnterMarkers;
  SmallVector<CallInst *, 4> FrameLeaveMarkers;
  SmallVector<CallInst *, 4> FrameSetCurrentMarkers;
  SmallVector<CallInst *, 4> KeepAliveMarkers;
  SmallVector<CallInst *, 4> StackObjectMarkers;
  for (Instruction &I : instructions(F)) {
    auto *CI = dyn_cast<CallInst>(&I);
    Function *Callee = CI ? CI->getCalledFunction() : nullptr;
    if (!Callee)
      continue;
    StringRef Name = Callee->getName();
    if (Name == ReturnSlotMarkerName)
      ReturnSlotMarkers.push_back(CI);
    else if (Name == FrameEnterMarkerName)
      FrameEnterMarkers.push_back(CI);
    else if (Name == FrameLeaveMarkerName)
      FrameLeaveMarkers.push_back(CI);
    else if (Name == FrameSetCurrentMarkerName)
      FrameSetCurrentMarkers.push_back(CI);
    else if (Name == KeepAliveMarkerName)
      KeepAliveMarkers.push_back(CI);
    else if (Name == StackObjectMarkerName)
      StackObjectMarkers.push_back(CI);
  }

  auto eraseFrameMarkers = [&]() {
    for (CallInst *CI : ReturnSlotMarkers) {
      if (!CI->use_empty())
        fail(Twine("a ") + ReturnSlotMarkerName + " marker in function '" +
             F.getName() + "' was not lowered");
      CI->eraseFromParent();
    }
    for (ArrayRef<CallInst *> Markers :
         {ArrayRef(FrameEnterMarkers), ArrayRef(FrameLeaveMarkers),
          ArrayRef(FrameSetCurrentMarkers), ArrayRef(StackObjectMarkers),
          ArrayRef(KeepAliveMarkers), ArrayRef(CalleeExitMarkers)}) {
      for (CallInst *CI : Markers)
        CI->eraseFromParent();
    }
  };

  if (FrameEnterMarkers.size() > 1)
    fail("function '" + F.getName() + "' has " +
         Twine(FrameEnterMarkers.size()) + " " + FrameEnterMarkerName +
         " markers, expected at most one");

  SmallSetVector<Value *, 8> ReturnSlotAliases;
  for (CallInst *CI : ReturnSlotMarkers)
    ReturnSlotAliases.insert(CI);
  for (unsigned I = 0; I < ReturnSlotAliases.size(); ++I) {
    for (User *U : ReturnSlotAliases[I]->users()) {
      if (isa<PHINode>(U) || isa<SelectInst>(U))
        ReturnSlotAliases.insert(U);
    }
  }

  bool ReturnSlotOnlyWritten = true;
  SmallPtrSet<Instruction *, 4> ReturnSlotWriters;
  SmallSetVector<Value *, 8> PlainRoots;
  PlainRootsMap PlainRootsByBase;
  for (Value *Slot : ReturnSlotAliases) {
    for (User *U : Slot->users()) {
      if (isa<PHINode>(U) || isa<SelectInst>(U))
        continue;
      Instruction *X = nullptr;
      if (auto *SI = dyn_cast<StoreInst>(U);
          SI && SI->getPointerOperand() == Slot) {
        ReturnSlotWriters.insert(SI);
        X = dyn_cast<Instruction>(SI->getValueOperand());
      } else if (auto *Call = dyn_cast<CallBase>(U);
                 Call && Call->hasArgument(Slot)) {
        ReturnSlotWriters.insert(Call);
        X = Call;
      } else {
        ReturnSlotOnlyWritten = false;
        continue;
      }
      // A store sunk below a merge writes a phi or select; the objects are its
      // leaves, and MergeBases builds the merged base from them.
      SmallVector<Value *, 4> Worklist = {X};
      SmallPtrSet<Value *, 4> Visited;
      while (!Worklist.empty()) {
        auto *Leaf = dyn_cast_or_null<Instruction>(Worklist.pop_back_val());
        if (!Leaf || !Visited.insert(Leaf).second)
          continue;
        if (isa<PHINode, SelectInst>(Leaf)) {
          append_range(Worklist, Leaf->operand_values());
          continue;
        }
        if (!Leaf->getType()->isPointerTy() ||
            isGCReferenceType(Leaf->getType()) || isStackOrStaticObject(Leaf) ||
            !DT->isReachableFromEntry(Leaf->getParent()) ||
            tracesToGCReference(Leaf))
          continue;
        if (PlainRoots.insert(Leaf))
          PlainRootsByBase[stripOffsets(Leaf)].push_back(Leaf);
      }
    }
  }
  auto isReturnSlotAlias = [&](Value *V) {
    return ReturnSlotAliases.contains(V);
  };
  for (Value *A : ReturnSlotAliases) {
    if (auto *PN = dyn_cast<PHINode>(A))
      ReturnSlotOnlyWritten &= all_of(PN->incoming_values(), isReturnSlotAlias);
    else if (auto *Sel = dyn_cast<SelectInst>(A))
      ReturnSlotOnlyWritten &= isReturnSlotAlias(Sel->getTrueValue()) &&
                               isReturnSlotAlias(Sel->getFalseValue());
  }

  splitRootInvokeEdges(F, PlainRoots, *DT);
  MergeBases Bases(F, *DT, PlainRoots, PlainRootsByBase);

  auto isCoveredByPlainRoot = [&](Value *V) {
    auto *ASC = dyn_cast<AddrSpaceCastInst>(V);
    return ASC && !isGCReferenceType(ASC->getSrcTy()) &&
           !Bases.isSynthesized(ASC) &&
           Bases.findPlainRoot(ASC->getPointerOperand(), ASC);
  };

  SmallVector<Value *, 16> CandidateRoots;
  SmallVector<Value *, 4> AggregateRoots;
  auto classify = [&](Value *V) {
    if (isCandidateRoot(V)) {
      if (!isCoveredByPlainRoot(V))
        CandidateRoots.push_back(V);
    } else if (!V->getType()->isPointerTy() &&
               containsGCReferenceType(V->getType()) && !isDerivedPointer(V)) {
      // A vector of derived pointers keeps its bases alive like a scalar one.
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

  SmallVector<CallBase *, 16> ClearPoints;
  for (auto *Markers : {&CalleeExitMarkers, &FrameSetCurrentMarkers}) {
    for (CallInst *CI : *Markers) {
      if (DT->isReachableFromEntry(CI->getParent()))
        ClearPoints.push_back(CI);
    }
  }

  SmallVector<Value *, 16> LiveRoots;
  DenseMap<Value *, SmallVector<CallBase *, 8>> SafepointsForRoot;
  DenseMap<Value *, SmallVector<CallBase *, 8>> ClearPointsForRoot;
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
      for (CallBase *P : ClearPoints) {
        if (Liveness.isLiveAcross(P))
          ClearPointsForRoot[V].push_back(P);
      }
    }
  }

  // The optimizer can keep references in vectors, such as when the SLP
  // vectorizer combines adjacent field loads. Root every reference such a
  // value holds for as long as the value is live.
  for (Value *V : AggregateRoots) {
    if (isa<Argument>(V) && !IsEntryFromC)
      continue;
    RootLiveness Liveness(V, *DT);
    SmallVector<CallBase *, 8> CoveringSafepoints;
    for (CallBase *S : Safepoints) {
      if (Liveness.isLiveAcross(S))
        CoveringSafepoints.push_back(S);
    }
    if (CoveringSafepoints.empty())
      continue;
    auto cannotRoot = [&]() {
      std::string Msg;
      raw_string_ostream OS(Msg);
      OS << "in function '" << F.getName() << "', a value of type "
         << *V->getType()
         << " holds references across a safepoint and cannot be rooted";
      fail(Msg);
    };
    Instruction *InsertPt = getStorePoint(V);
    if (!InsertPt)
      cannotRoot();
    IRBuilder<> Builder(InsertPt);
    SmallVector<Value *, 4> Refs;
    if (!extractReferences(Builder, V, Refs))
      cannotRoot();
    for (Value *Ref : Refs) {
      if (isa<Constant>(Ref) || (isa<Argument>(Ref) && !IsEntryFromC))
        continue;
      if (!isCandidateRoot(Ref))
        cannotRoot();
      auto [It, Inserted] = SafepointsForRoot.try_emplace(Ref);
      if (Inserted)
        LiveRoots.push_back(Ref);
      for (CallBase *S : CoveringSafepoints) {
        if (!is_contained(It->second, S))
          It->second.push_back(S);
      }
      for (CallBase *P : ClearPoints) {
        if (Liveness.isLiveAcross(P) && !is_contained(ClearPointsForRoot[Ref], P))
          ClearPointsForRoot[Ref].push_back(P);
      }
    }
  }

  if (LiveRoots.empty() && ReturnSlotMarkers.empty() &&
      StackObjectMarkers.empty() && FrameSetCurrentMarkers.empty()) {
    eraseFrameMarkers();
    Bases.eraseUnused();
    lowerAddressSpaces(F);
    return false;
  }

  BasicBlock &EntryBB = F.getEntryBlock();
  DenseMap<Value *, unsigned> SlotAssignment;
  unsigned NumRootSlots =
      assignRootSlots(F, LiveRoots, SafepointsForRoot, *DT, SlotAssignment);
  unsigned ReturnSlotIdx = NumRootSlots;
  unsigned NumSlots = NumRootSlots + (ReturnSlotMarkers.empty() ? 0 : 1);

  DenseMap<Value *, unsigned> StackObjectSlots;
  // The optimizer can merge the headers of several stack objects into one
  // marker, such as when it sinks the markers into a common successor. The
  // lifetime end of each such object then clears the slot only while it
  // still holds that object.
  DenseMap<Value *, SmallVector<unsigned, 1>> MergedStackObjectSlots;
  for (CallInst *CI : StackObjectMarkers) {
    Value *Obj = getUnderlyingObject(CI->getArgOperand(0));
    if (!StackObjectSlots.insert({Obj, NumSlots}).second)
      continue;
    if (!isa<AllocaInst>(Obj)) {
      SmallVector<const Value *, 4> Objs;
      getUnderlyingObjects(Obj, Objs);
      for (const Value *O : Objs) {
        if (O != Obj && isa<AllocaInst>(O))
          MergedStackObjectSlots[const_cast<Value *>(O)].push_back(NumSlots);
      }
    }
    ++NumSlots;
  }

  Module *Mod = F.getParent();
  unsigned OverlaySlots = getFrameOverlaySlots(*Mod);
  unsigned TotalSlots = OverlaySlots + NumSlots;

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

  IRBuilder<> EntryBuilder(getFirstNonAlloca(EntryBB));

  ArrayType *FrameArrayTy = ArrayType::get(Ptr0Ty, TotalSlots);
  AllocaInst *FrameAlloca =
      EntryBuilder.CreateAlloca(FrameArrayTy, nullptr, "shadow_stack_frame");
  FrameAlloca->setAlignment(Align(8));

  for (Instruction &I : instructions(F)) {
    if (auto *CI = dyn_cast<CallInst>(&I); CI && CI->isMustTailCall())
      fail("function '" + F.getName() +
           "' needs a frame but contains a musttail call");
  }

  uint64_t FrameBytes = TotalSlots * DL.getPointerSize();
  withDebugLoc(EntryBuilder.CreateMemSet(
      FrameAlloca, ConstantInt::get(Type::getInt8Ty(Ctx), 0), FrameBytes,
      Align(8)));

  FunctionCallee EnterFrameFunc = getOrCreateFrameFunc(*Mod, "EnterFrame");
  IRBuilder<> EnterBuilder(FrameEnterMarkers.empty()
                               ? &*EntryBuilder.GetInsertPoint()
                               : FrameEnterMarkers.front());
  withDebugLoc(EnterBuilder.CreateCall(
      EnterFrameFunc, {FrameAlloca, ConstantInt::get(Int32Ty, 0),
                       ConstantInt::get(Int32Ty, TotalSlots)}));

  auto getSlotPtr = [&](IRBuilder<> &B, unsigned SlotIdx) -> Value * {
    return B.CreateConstInBoundsGEP2_32(FrameArrayTy, FrameAlloca, 0,
                                        OverlaySlots + SlotIdx,
                                        "slot_" + Twine(SlotIdx));
  };

  Value *ReturnSlotPtr = nullptr;
  if (!ReturnSlotMarkers.empty()) {
    ReturnSlotPtr = getSlotPtr(EntryBuilder, ReturnSlotIdx);
    for (CallInst *CI : ReturnSlotMarkers)
      CI->replaceAllUsesWith(ReturnSlotPtr);
  }

  DenseMap<Instruction *, unsigned> SlotWrites;
  for (Value *V : LiveRoots) {
    unsigned Slot = SlotAssignment[V];
    Instruction *InsertPt =
        isa<Argument>(V) ? &*EntryBuilder.GetInsertPoint() : getStorePoint(V);
    if (!InsertPt)
      fail("cannot root the result of a terminator in function '" +
           F.getName() + "'");
    IRBuilder<> Builder(InsertPt);
    SlotWrites[Builder.CreateStore(V, getSlotPtr(Builder, Slot))] = Slot;
  }

  for (CallInst *CI : StackObjectMarkers) {
    Value *Header = CI->getArgOperand(0);
    unsigned Slot = StackObjectSlots.lookup(getUnderlyingObject(Header));
    IRBuilder<> Builder(CI);
    Builder.CreateStore(Header, getSlotPtr(Builder, Slot));
  }
  if (!StackObjectSlots.empty()) {
    Constant *Null = ConstantPointerNull::get(cast<PointerType>(Ptr0Ty));
    for (Instruction &I : instructions(F)) {
      auto *II = dyn_cast<IntrinsicInst>(&I);
      if (!II || II->getIntrinsicID() != Intrinsic::lifetime_end)
        continue;
      Value *Obj = getUnderlyingObject(II->getArgOperand(II->arg_size() - 1));
      if (auto It = StackObjectSlots.find(Obj); It != StackObjectSlots.end()) {
        IRBuilder<> Builder(II);
        Builder.CreateStore(Null, getSlotPtr(Builder, It->second));
      }
      if (auto It = MergedStackObjectSlots.find(Obj);
          It != MergedStackObjectSlots.end()) {
        IRBuilder<> Builder(II);
        for (unsigned Slot : It->second) {
          Value *SlotPtr = getSlotPtr(Builder, Slot);
          Value *Held = Builder.CreateLoad(Ptr0Ty, SlotPtr, "held");
          Value *HoldsObj = Builder.CreateICmpEQ(Held, Obj, "holds_obj");
          Builder.CreateStore(Builder.CreateSelect(HoldsObj, Null, Held),
                              SlotPtr);
        }
      }
    }
  }

  if (!ClearPoints.empty() && (NumRootSlots > 0 || ReturnSlotPtr)) {
    if (ReturnSlotPtr && ReturnSlotOnlyWritten) {
      for (Instruction *W : ReturnSlotWriters)
        SlotWrites[W] = ReturnSlotIdx;
    }
    emitSlotClears(F, Safepoints, ClearPoints, LiveRoots, ClearPointsForRoot,
                   SlotAssignment, SlotWrites, ReturnSlotIdx + 1, getSlotPtr);
  }

  FunctionCallee LeaveFrameFunc = getOrCreateFrameFunc(*Mod, "LeaveFrame");
  auto emitLeaveFrame = [&](Instruction *InsertBefore) {
    IRBuilder<> Builder(InsertBefore);
    withDebugLoc(Builder.CreateCall(LeaveFrameFunc,
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
    if (const Instruction *Exit = findReachableInst(
            &EntryBB.front(),
            [](const Instruction *I) {
              return isa<ReturnInst>(I) || isa<ResumeInst>(I);
            },
            [&](const Instruction *I) { return LeaveMarkerSet.contains(I); })) {
      fail("exit '" + Exit->getParent()->getName() + "' of function '" +
           F.getName() + "' is reachable without passing a " +
           FrameLeaveMarkerName + " marker");
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
  SmallPtrSet<const Instruction *, 4> SetCurrentMarkerSet(
      FrameSetCurrentMarkers.begin(), FrameSetCurrentMarkers.end());
  for (CallInst *CI : FrameSetCurrentMarkers) {
    IRBuilder<> MarkerBuilder(CI);
    withDebugLoc(MarkerBuilder.CreateCall(SetCurrentFrameFunc, {FrameAlloca}));
  }

  for (BasicBlock &BB : F) {
    auto *LP = BB.getLandingPadInst();
    if (!LP)
      continue;
    if (!LP->isCleanup())
      LP->setCleanup(true);

    if (!DT->isReachableFromEntry(&BB) ||
        !LP->getMetadata(KotlinLandingPadMetadataName))
      continue;

    if (const Instruction *Call = findReachableInst(
            LP->getNextNode(),
            [](const Instruction *I) {
              auto *CB = dyn_cast<CallBase>(I);
              const Function *Callee = CB ? CB->getCalledFunction() : nullptr;
              return Callee && Callee->hasFnAttribute(KotlinGcFrameAttrName);
            },
            [&](const Instruction *I) {
              return SetCurrentMarkerSet.contains(I);
            })) {
      fail("landing pad '" + BB.getName() + "' of function '" + F.getName() +
           "' reaches a call to '" +
           cast<CallBase>(Call)->getCalledFunction()->getName() +
           "' without passing a " + FrameSetCurrentMarkerName + " marker");
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
  auto inlineCalls = [&](Function *Callee, ArrayRef<CallBase *> Calls) {
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
    if (Callee->hasLocalLinkage() && Callee->use_empty())
      Callee->eraseFromParent();
  };
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
    inlineCalls(Helper, Calls);
  }
  // Codegen keeps the thread state switches out of line, so that the pass sees
  // the native thread state. Inline them where the optimizer would have.
  for (const char *Name : {"Kotlin_mm_switchThreadStateNative",
                           "Kotlin_mm_switchThreadStateRunnable"}) {
    Function *Switch = M.getFunction(Name);
    if (!Switch)
      continue;
    bool AlwaysInline = !Switch->isDeclaration() &&
                        Switch->hasFnAttribute(Attribute::AlwaysInline);
    SmallVector<CallBase *, 32> Calls;
    for (User *U : Switch->users()) {
      auto *CB = dyn_cast<CallBase>(U);
      if (!CB || CB->getCalledFunction() != Switch ||
          !CB->getAttributes().hasFnAttr(Attribute::NoInline))
        continue;
      CB->removeFnAttr(Attribute::NoInline);
      if (AlwaysInline)
        Calls.push_back(CB);
    }
    inlineCalls(Switch, Calls);
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

  for (const char *Name : MarkerNames) {
    Function *Marker = M.getFunction(Name);
    if (!Marker)
      continue;
    if (!Marker->use_empty()) {
      if (auto *I = dyn_cast<Instruction>(*Marker->user_begin()))
        fail(Twine(Marker->getNumUses()) + " unlowered call(s) to " + Name +
             " remain, first in function '" + I->getFunction()->getName() +
             "'");
      fail(Twine(Marker->getNumUses()) + " unlowered call(s) to " + Name +
           " remain");
    }
    Marker->eraseFromParent();
  }
  return Changed;
}

extern "C" void LLVMKotlinBuildShadowStack(LLVMModuleRef M) {
  buildShadowStackOnModule(*unwrap(M));
}

PreservedAnalyses BuildShadowStackPass::run(Module &M,
                                            ModuleAnalysisManager &) {
  buildShadowStackOnModule(M);
  return PreservedAnalyses::none();
}
