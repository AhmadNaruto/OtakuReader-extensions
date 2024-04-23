package ireader.worldnovel

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
abstract class WorldNovel(deps: Dependencies) : SourceFactory(
    deps = deps,
) {
    override val lang: String
        get() = "in"
    override val baseUrl: String
        get() = "https://worldnovel.online"
    // override val id: Long
    //    get() = 71
    override val name: String
        get() = "WorldNovel"

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
                endpoint = "/find-series/page/{page}/?is_search=true&status=&tahun=&filter_publisher=&filter_cnt=&orderby=date&order=DESC",
                selector = "div.mb-4 > row > .col-12",
                nameSelector = ".col-6 > img",
                nameAtt = "alt",
                linkSelector = "p.post-title a",
                linkAtt = "href",
                coverSelector = ".col-6 > img",
                coverAtt = "src",
                maxPage = 10
            ),
        )

    override val detailFetcher: Detail
        get() = SourceFactory.Detail(
            nameSelector = "li[aria-current=page]",
            coverSelector = ".d-flex .mb-4 > img",
            coverAtt = "src",
            authorBookSelector = ".mb-4:contains(Author) li"
            descriptionSelector = "#maintab div.text-cloud",
        )
        
// iam strugle with this not implemented
    override val chapterFetcher: Chapters
        get() = SourceFactory.Chapters(
            selector = "section div.container",
            nameSelector = "a",
            nameAtt = "title",
            linkSelector = "a",
            linkAtt = "href",
            reverseChapterList = true,
        )

    override val contentFetcher: Content
        get() = SourceFactory.Content(
            pageTitleSelector = "h3.post-title",
            pageContentSelector = "div.post-content",
        )

}
