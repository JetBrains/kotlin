# `ILightLazyParseableElementType` on `BLOCK`/`LAMBDA_EXPRESSION`: effect on IDE incremental reparse

Context: [light-stub-conversion.md](light-stub-conversion.md). To build stubs from a *lazily* parsed light tree, `BLOCK`
(`BlockExpressionElementType`) and `LAMBDA_EXPRESSION` (`LambdaExpressionElementType`) now implement
`ILightLazyParseableElementType`. This note explains why that is needed, and how it changes incremental reparse in the IDE,
which has nothing to do with stub building.

Platform sources referenced below are in the monorepo (`~/work/dev/community/platform/...`). The behavior is derived from
reading that code. It has **not** been exercised in the IDE yet.

## Why it is needed

In the light tree (`PsiBuilderImpl.MyTreeStructure.insertLeaf`), a collapsed marker becomes:

- a `LazyParseableToken` if its type is `ILightLazyParseableElementType`. It implements `LighterLazyParseableNode`, and
  `getChildren` parses its contents on demand (`parseContents`, cached in `myParsed`; the token itself is cached in
  `builder.myChameleonCache`);
- otherwise a plain `TokenRangeNode`, a leaf with **no children**.

Without the interface, a lazy light parse would hide every declaration inside a block (local classes, functions in
lambdas, ...) from the stub builder. Also, `skipChildProcessingWhenBuildingStubs` couldn't use
`LighterLazyParseableNode.accept` to scan already-lexed tokens. The previous approach of parsing the light tree eagerly cost
~44 s vs ~25.8 s on the monorepo.

## Where else the interface matters: the reparse diff

Incremental reparse (`BlockSupportImpl.reparse`) has two shapes:

- Reparse the innermost reparseable node: `BLOCK` and `LAMBDA_EXPRESSION` are `IErrorCounterReparseableElementType`s, and the
  node is reparsed if its braces stay balanced.
- Otherwise, `makeFullParse` reparses the whole file.

Both end up in `PsiBuilderImpl.merge`. The new text is parsed into a light tree and diffed against the old AST
(`BlockSupportImpl.diffTrees` / `DiffTree.build`) with `PsiBuilderImpl.MyComparator`. Kotlin's parser is lazy here too, so
nested blocks and lambdas in the new tree are collapsed tokens. `MyComparator.deepEqual` decides what happens to them:

```java
if (type instanceof ILightLazyParseableElementType) {                                    // new branch for Kotlin blocks
  if (((TreeElement)oldNode).textMatches(token.getText())) {
    return PsiDocumentManagerBase.isFullReparseInProgress() ? ThreeState.UNSURE : ThreeState.YES;
  }
  return TreeUtil.isCollapsedChameleon(oldNode)
         ? ThreeState.NO  // do not dive into collapsed nodes
         : ThreeState.UNSURE;
}

if (oldNode.getElementType() instanceof ILazyParseableElementType && type instanceof ILazyParseableElementType || ...) {
  return ((TreeElement)oldNode).textMatches(token.getText()) ? ThreeState.YES : ThreeState.NO;  // old branch
}
```

In `DiffTree.looksEqual`, `YES` means the node is kept, `NO` means it is replaced as a whole (`nodeReplaced`), and `UNSURE` means
"drill down": the children are diffed recursively.

### Behavior before and after, per nested block/lambda

| Old node in the AST                   | Text      | Before (`ILazyParseableElementType`) | After (`ILightLazyParseableElementType`)                     |
|---------------------------------------|-----------|--------------------------------------|--------------------------------------------------------------|
| any                                   | unchanged | kept                                 | kept                                                         |
| any, during `reparseFileFromText`     | unchanged | kept                                 | **drill down**: both old and new block contents are parsed    |
| collapsed (never parsed)              | changed   | replaced                             | replaced (same)                                              |
| expanded (already parsed AST)         | changed   | replaced as a whole                  | **drill down**: new block light-parsed, children diffed      |

Java's `JavaElementType` code block is `ICompositeElementType, ILightLazyParseableElementType`, so Kotlin now behaves like
Java here.

## Consequences

- **Finer-grained PSI preservation (likely the main visible effect).** When a block that was already expanded changes, its
  unchanged statements keep their PSI identity, and only the changed subtrees are replaced. Before, the whole block was
  replaced, so all PSI inside it was invalidated and the new block stayed collapsed until accessed. This mostly affects edits
  where the innermost block reparse is not possible (unbalanced braces while typing, or edits spanning several blocks), so the
  reparse happens at an outer block or the whole file.
- **Different PSI change events.** Listeners now see replacements deep inside a body instead of a replacement of the whole
  `BLOCK`. Anything classifying modifications by the changed element has to handle both shapes, e.g. the Kotlin plugin's
  in-block vs out-of-block modification tracking that Analysis API / LL FIR session invalidation relies on. Both shapes are
  inside a body, so classification should not change, but this is worth testing rather than assuming.
- **Cost shifts.**
  - Changed expanded blocks are now parsed (light) during the commit instead of lazily afterwards, but their unchanged
    subtrees are not rebuilt.
  - `reparseFileFromText` (full reparse that preserves PSI) now drills into *every* unchanged block. That parses old
    collapsed blocks (`ASTStructure.getChildren`) and new ones, so it costs more on large files. It is the same trade-off
    Java already makes.
- **Correctness depends on light/AST tree shape equality inside blocks.** Drilling down compares the light parse of a block
  (`KotlinLightParser.parseBlockExpression`/`parseLambdaExpression` via `parseContents(LighterLazyParseableNode)`) with the
  AST parse (`parseContents(ASTNode)`). Children of the nested light root become the chameleon's children
  (`MyTreeStructure.getChildren` for `LazyParseableToken`). So the light root must match the AST block node: `BLOCK` /
  `LAMBDA_EXPRESSION` with `{`, statements, `}` as children.
  - The 209k-file stub sweep only checks what stubs read (declarations), not every node, comment or error element.
  - A mismatch would show up as a wrong incremental reparse (incremental result ≠ full reparse) and the usual PSI/document
    consistency errors.
- **Other light tree consumers.** Code that reads the light tree from `parseContentsLight` now sees collapsed blocks that
  expand on `getChildren`, instead of an eagerly parsed tree. This is transparent for `LighterAST` users, but a consumer
  checking `LighterLazyParseableNode`/`ILightLazyParseableElementType` explicitly would see a difference. The compiler's
  light-tree frontend is unaffected: it parses with `createForTopLevelNonLazy`, so it has no collapsed nodes.

## Options

1. **Keep it (aligned with Java).** Verify in the Kotlin plugin (intellij-community):
   - incremental reparse / PSI consistency tests;
   - Kotlin code-block modification tracking tests;
   - typing in bodies with unbalanced braces;
   - "reload from disk" on large files (`reparseFileFromText`).
2. **Keep the light laziness but restore the old diff semantics** with a Kotlin `CustomLanguageASTComparator`
   (`com.intellij.tree.CustomLanguageASTComparator`, language extension). `MyComparator.deepEqual` consults it *before* the
   `ILightLazyParseableElementType` branch. Return `YES`/`NO` by text match for `BLOCK`/`LAMBDA_EXPRESSION` tokens, and
   `UNSURE` for everything else. This keeps the IDE behavior exactly as before, at the cost of one more extension.
3. **Back out the interface** and go back to an eager light parse for stubs: no IDE impact, ~44 s instead of ~25.8 s on the
   monorepo, i.e. slower than the AST builder (~30.8 s). Not really viable.

Option 2 is the conservative choice if the platform/IDE owners don't want a reparse behavior change bundled with the stub
builder. Option 1 is probably better for the IDE long-term, but should land with its own tests.
