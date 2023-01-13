package test

class PublicClass1
class PublicClass2

typealias PublicTypeAlias1 = PublicClass1
typealias PublicTypeAlias2 = PublicClass1

internal typealias InternalTypeAlias1 = PublicClass1
internal typealias InternalTypeAlias2 = PublicClass1

private typealias PrivateTypeAlias1 = PublicClass2
