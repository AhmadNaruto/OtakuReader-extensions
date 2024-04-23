package ireader.sakuranovel

import io.ktor.client.request.post
import ireader.core.source.Dependencies
import ireader.core.source.asJsoup
import ireader.core.source.model.ChapterInfo
import ireader.core.source.model.Command
import ireader.core.source.model.CommandList
import ireader.core.source.model.Filter
import ireader.core.source.model.FilterList
import ireader.core.source.model.MangaInfo
import ireader.core.source.SourceFactory
import tachiyomix.annotations.Extension

@Extension
abstract class SakuraNovel(deps: Dependencies) : SourceFactory(
    deps = deps,
) {
    override val lang: String
        get() = "en"
    override val baseUrl: String
        get() = "https://sakuranovel.id"
    override val id: Long
        get() = 73
    override val name: String
        get() = "SakuraNovel"

    override fun getFilters(): FilterList = listOf(
        Filter.Title(),
    )

    override fun getCommands(): CommandList = listOf(
        Command.Detail.Fetch(),
        Command.Content.Fetch(),
        Command.Chapter.Fetch(),
    )


    override val exploreFetchers: List<BaseExploreFetcher>
        get() = listOf(
            BaseExploreFetcher(
                "Latest",
                endpoint = "/page/{page}/",
                selector = ".flexbox3 .flexbox3-item",
                nameSelector = ".title > a",
                //nameAtt = "title",
                linkSelector = ".title > a",
                linkAtt = "href",
                coverSelector = "flexbox3-thumb > img",
                coverAtt = "src",
                maxPage = 10
            ),
            BaseExploreFetcher(
                "Search",
                endpoint = "/page/{page}/?s={query}",
                selector = ".flexbox2 .flexbox2-item",
                nameSelector = "flexbox2-content > a",
                nameAtt = "title",
                linkSelector = "flexbox2-content > a",
                linkAtt = "href",
                coverSelector = "flexbox2-thumb > img",
                coverAtt = "src",
                maxPage = 10,
                type = SourceFactory.Type.Search
            ),
        )

    override val detailFetcher: Detail
        get() = SourceFactory.Detail(
            nameSelector = ".series-titlex > h2",
            coverSelector = ".series-thumb > img",
            coverAtt = "src",
            descriptionSelector = ".series-synops p",
        )

    override val chapterFetcher: Chapters
        get() = SourceFactory.Chapters(
            selector = ".series-chapterlist > li 
            .flexch-infoz",
            nameSelector = "a",
            nameAtt = "title",
            linkSelector = "a",
            linkAtt = "href",
            reverseChapterList = true,
        )

    override val contentFetcher: Content
        get() = SourceFactory.Content(
            pageTitleSelector = ".text-story > h2",
            pageContentSelector = ".text-story",
        )

}
