package com.intellij.compiler.artifacts;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.ui.ArtifactProblemsHolderBase;
import com.intellij.packaging.ui.ArtifactProblemQuickFix;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author nik
 */
public abstract class PackagingValidationTestCase extends PackagingElementsTestCase {
  protected PackagingValidationTestCase() {
    mySetupModule = true;
  }

  protected MockArtifactProblemsHolder validate(CompositePackagingElement<?> root, final ArtifactType artifactType) {
    final MockArtifactProblemsHolder validationManager = new MockArtifactProblemsHolder();
    final Artifact artifact = addArtifact("artifact", artifactType, root);
    artifactType.checkRootElement(root, artifact, validationManager);
    return validationManager;
  }


  protected class MockArtifactProblemsHolder extends ArtifactProblemsHolderBase {
    private final List<String> myProblems = new ArrayList<>();
    private final Map<String, ArtifactProblemQuickFix[]> myQuickFixes = new THashMap<>();

    public MockArtifactProblemsHolder() {
      super(new MockPackagingEditorContext(new MockArtifactsStructureConfigurableContext(), null));
    }

    @Override
    public void registerError(@NotNull String message,
                              @NotNull String problemTypeId,
                              @Nullable List<PackagingElement<?>> pathToPlace,
                              @NotNull ArtifactProblemQuickFix... quickFixes) {
      myProblems.add(message);
      myQuickFixes.put(message, quickFixes);
    }

    @Override
    public void registerWarning(@NotNull String message,
                                @NotNull String problemTypeId, @Nullable List<PackagingElement<?>> pathToPlace,
                                @NotNull ArtifactProblemQuickFix... quickFixes) {
      registerError(message, problemTypeId, pathToPlace, quickFixes);
    }

    public void assertNoProblems() {
      assertProblems();
    }

    public void assertProblems(String... expectedMessages) {
      Set<String> expected = new THashSet<>(Arrays.asList(expectedMessages));
      outer:
      for (String problem : myProblems) {
        for (String message : expected) {
          if (problem.contains(message)) {
            expected.remove(message);
            continue outer;
          }
        }
        fail("Unexpected problem: " + problem);
      }
      if (!expected.isEmpty()) {
        fail("The following problems are not reported: " + StringUtil.join(expected, "; "));
      }
    }
  }
}
