internal typealias SectionMap = Map<String, Long>
internal typealias TypedSectionMap = Map<Int, SectionMap>

class SharedConfig internal constructor(internal val sections: TypedSectionMap)

internal fun mergeFiles(vararg maps: TypedSectionMap): TypedSectionMap = maps.first()

internal fun sectionsOf(config: SharedConfig): List<SectionMap> = config.sections.values.toList()
