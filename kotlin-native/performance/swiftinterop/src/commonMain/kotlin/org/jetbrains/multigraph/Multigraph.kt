/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.multigraph

import kotlin.math.pow

interface Cost {
    operator fun plus(other: Cost): Cost
    operator fun minus(other: Cost): Cost
    override operator fun equals(other: Any?): Boolean
    operator fun compareTo(other: Cost): Int
}

data class Edge<T>(val id: UInt, val from: T, val to: T, val cost: Cost) {
    override operator fun equals(other: Any?): Boolean {
        return if (other is Edge<*>) (from == other.from && to == other.to && cost == other.cost) else false
    }

    override fun hashCode(): Int =
            from.hashCode() * 31.0.pow(2.0).toInt() + to.hashCode() * 31 + cost.hashCode()
}

class EdgeAbsenceMultigraphException(message: String): Exception(message) {}
class VertexAbsenceMultigraphException(message: String): Exception(message) {}

class Multigraph<T>() {
    private var edges = mutableMapOf<T, MutableList<Edge<T>>>()
    private var idCounter = 0u

    val allVertexes: Set<T>
        get() {
            val outerVertexes = edges.keys
            val innerVertexes = edges.map { (_, values) ->
                values.map { it.to }.filter { it !in outerVertexes }
            }.flatten()
            return outerVertexes.union(innerVertexes)
        }

    val allEdges: List<UInt>
        get() = edges.values.flatten().map { it.id }

    fun getEdgeById(id: UInt): Edge<T> {
        edges.forEach { (_, value) ->
            value.forEach {
                if (it.id == id)
                    return it
            }
        }
        throw EdgeAbsenceMultigraphException("Edge with id $id wasn't found.")
    }

    fun copyMultigraph(): Multigraph<T> {
        val newInstance = Multigraph<T>()
        edges.forEach { (vertex, edges) ->
            edges.forEach { edge ->
                newInstance.addEdge(vertex, edge.to, edge.cost)
            }
        }
        return newInstance
    }

    fun addEdge(from: T, to: T, cost: Cost): UInt {
        val edge = Edge(idCounter, from, to, cost)
        edges.getOrPut(from) { mutableListOf() }.add(edge)
        idCounter++
        return edge.id
    }

    fun removeEdge(id: UInt) {
        try {
            val edge = getEdgeById(id)
            edges[edge.from]?.remove(edge)
        } catch (exception: EdgeAbsenceMultigraphException) {
            println("WARNING: no edge with id $id was found.")
        }
    }

    fun removeVertex(vertex: T) {
        edges.remove(vertex)
        val edgesToRemove = edges.map { (_, values) ->
            values.filter {
                it.to == vertex
            }
        }.flatten()
        edgesToRemove.forEach {
            removeEdge(it.id)
        }
    }

    fun checkVertexExistance(vertex: T): Boolean {
        return vertex in allVertexes
    }

    fun getTo(edgeId: UInt) =
            getEdgeById(edgeId).to

    fun getFrom(edgeId: UInt) =
            getEdgeById(edgeId).from

    fun getCost(edgeId: UInt) =
            getEdgeById(edgeId).cost

    fun getEdgesFrom(vertex: T) =
            edges[vertex] ?: listOf<Edge<T>>()

    fun isEmpty() = edges.isEmpty()

    fun searchRoutesWithLimits(start: T, finish: T, limits: Cost): List<List<UInt>> {
        data class WaveStep(val costs: MutableList<Cost> = mutableListOf(),
                            val routes: MutableList<MutableList<UInt>> = mutableListOf(),
                            val vertexes: MutableList<MutableList<T>> = mutableListOf())

        val currentStepsState = mutableMapOf<T, WaveStep>()
        var oldFront = mutableSetOf<T>(start)
        val newFront = mutableSetOf<T>()

        if (!checkVertexExistance(start)) {
            throw(VertexAbsenceMultigraphException("Start vertex wasn't found in graph."))
        }
        if (!checkVertexExistance(finish)) {
            throw(VertexAbsenceMultigraphException("Finish vertex wasn't found in graph."))
        }

        allVertexes.forEach {
            currentStepsState[it] = WaveStep()
        }
        while (!oldFront.isEmpty()) {
            oldFront.forEach {
                val currentStepState = currentStepsState[it] ?: WaveStep()
                // Lookup all edges from vertex.
                val values = edges.get(it) ?: mutableListOf<Edge<T>>()
                values.forEach { edge ->
                    val newRoutes = mutableListOf<UInt>()
                    val toStep = currentStepsState[edge.to] ?: WaveStep()

                    // Create new pathes and count their costs.
                    if (currentStepState.routes.isEmpty()) {
                        val newVertexes = mutableListOf(edge.from, edge.to)
                        val newCost = edge.cost
                        if (newCost <= limits && edge.from != edge.to) {
                            newRoutes.add(edge.id)
                            toStep.routes.add(newRoutes)
                            toStep.costs.add(edge.cost)
                            toStep.vertexes.add(newVertexes)
                            currentStepsState[edge.to] = toStep
                            // Add new wave front.
                            if (edge.to != finish && edge.to !in oldFront) {
                                newFront.add(edge.to)
                            }
                        }
                    } else {
                        currentStepState.routes.forEachIndexed { index, it ->
                            val newRoutes = it.toMutableList()
                            val oldCost = currentStepState.costs.get(index)
                            val newCost = edge.cost + oldCost
                            val newVertexes = currentStepState.vertexes.get(index).toMutableList()
                            if (newCost <= limits && edge.to !in currentStepState.vertexes.get(index)) {
                                newRoutes.add(edge.id)
                                newVertexes.add(edge.to)
                                var addNewSequence = true

                                if (newRoutes !in toStep.routes) {
                                    toStep.routes.add(newRoutes)
                                    toStep.costs.add(newCost)
                                    toStep.vertexes.add(newVertexes)
                                    currentStepsState[edge.to] = toStep
                                    if (edge.to != finish && edge.to !in oldFront) {
                                        newFront.add(edge.to)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            oldFront = newFront.toMutableSet()
            newFront.clear()
        }
        return currentStepsState[finish]?.routes ?: listOf<List<UInt>>()
    }
}