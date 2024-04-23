package ireader.novelku


import ireader.madara.Madara
import ireader.core.source.Dependencies
import ireader.madara.Path
import tachiyomix.annotations.Extension

@Extension
abstract class Novelku(val deps: Dependencies) : Madara(
    deps,
    key = "https://novelku.id",
    sourceName = "novelku",
    sourceId = 91,
    language = "in",
)
