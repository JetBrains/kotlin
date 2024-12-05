/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import java.net.URLClassLoader
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import java.io.File
import java.net.URL


class AetherResolver {
    fun newRepositorySystem(): RepositorySystem {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)

// RepositorySystem initialization
        val repositorySystem: RepositorySystem = locator.getService(RepositorySystem::class.java)
            ?: throw IllegalStateException("RepositorySystem could not be initialized")
        return repositorySystem
//        val locator = org.eclipse.aether.impl.DefaultServiceLocator()
//        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
//        locator.addService(org.eclipse.aether.spi.connector.transport.TransporterFactory::class.java, FileTransporterFactory::class.java)
//        locator.addService(org.eclipse.aether.spi.connector.transport.TransporterFactory::class.java, HttpTransporterFactory::class.java)
//        return locator.getService(RepositorySystem::class.java)
    }

    // Create the RepositorySystemSession
    fun newSession(system: RepositorySystem): RepositorySystemSession {
        val session = DefaultRepositorySystemSession()
        val localRepo = LocalRepository(System.getProperty("user.home") + "/.m2/repository")
        session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)
        session.dependencySelector = ExclusionDependencySelector() // Exclude unwanted dependencies
        return session
    }

    // Remote repositories (e.g., Maven Central)
    val central = RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build()
    fun resolveDependencies(
        system: RepositorySystem,
        session: RepositorySystemSession,
        artifactCoordinates: String
    ): List<File> {
        val artifactParts = artifactCoordinates.split(":")
        val artifact = org.eclipse.aether.artifact.DefaultArtifact(
            artifactParts[0], artifactParts[1], "jar", artifactParts[2]
        )

        val collectRequest = CollectRequest().apply {
            root = Dependency(artifact, "")
            addRepository(central)
        }

        val dependencyRequest = DependencyRequest(collectRequest, null)
        val result = system.resolveDependencies(session, dependencyRequest)
        return result.artifactResults.map { it.artifact.file }
    }
    fun addJarsToClassLoader(jars: List<File>, classLoader: ClassLoader): URLClassLoader {
        val urls = jars.map { it.toURI().toURL() }.toTypedArray()
        return URLClassLoader(urls, classLoader)
    }

    fun resolveArtifact(artifactCoordinates: String): Array<URL> {
        val system = newRepositorySystem()
        val session = newSession(system)

        val dependencies = resolveDependencies(system, session, artifactCoordinates)
        val urls = dependencies.mapNotNull { it.toURI().toURL() }.toTypedArray()
        return urls
//        val updatedClassLoader = addJarsToClassLoader(dependencies, ClassLoader.getSystemClassLoader())
//        return updatedClassLoader
    }
}