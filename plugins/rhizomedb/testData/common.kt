package com.jetbrains.rhizomedb

interface Entity

abstract class EntityType<T : Entity>()

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GeneratedEntityType()
