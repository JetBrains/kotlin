package org.jetbrains.kotlin.gradle.plugin.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification


class BaseKonanSpecification extends Specification {

    @Rule
    TemporaryFolder tmpFolder = new TemporaryFolder()
    File getProjectDirectory() { return tmpFolder.root }

}
