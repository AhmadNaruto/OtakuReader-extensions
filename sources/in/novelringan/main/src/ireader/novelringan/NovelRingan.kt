package ireader.novelringan

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
abstract class NovelRingan(deps: Dependencies) : SourceFactory(
    deps = deps,
) {
    override val lang: String
        get() = "in"
    override val baseUrl: String
        get() = "https://novelringan.com"
    override val id: Long
        get() = 92
    override val name: String
        get() = "NovelRingan"

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
                endpoint = "/advanced-search/page/{page}/?title=&status=&order=update",
                selector = ".listupd article",
                nameSelector = "h2 a",
                // nameAtt = "title",
                linkSelector = "h2 a",
                linkAtt = "href",
                coverSelector = "img",
                coverAtt = "src",
                maxPage = 10
            ),
            BaseExploreFetcher(
                "Search",
                endpoint = "/advanced-search/page/{page}/?title={query}&status=&order=update",
                selector = ".listupd article",
                nameSelector = "h2 a",
                // nameAtt = "title",
                linkSelector = "h2 a",
                linkAtt = "href",
                coverSelector = "img",
                coverAtt = "src",
                maxPage = 10,
                type = SourceFactory.Type.Search
            ),
        )

    override val detailFetcher: Detail
        get() = SourceFactory.Detail(
            nameSelector = ".maininfo",
            coverSelector = ".imgprop img",
            coverAtt = "src",
            descriptionSelector = "span p",
        )

    override val chapterFetcher: Chapters
        get() = SourceFactory.Chapters(
            selector = ".bxcl ul li",
            nameSelector = "a",
            // nameAtt = "title",
            linkSelector = "a",
            linkAtt = "href",
            reverseChapterList = true,
        )

    override val contentFetcher: Content
        get() = SourceFactory.Content(
            pageTitleSelector = ".entry-title",
            pageContentSelector = ".entry-content",
        )

}
