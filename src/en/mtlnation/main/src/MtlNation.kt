package ireader.mtlnation

import android.util.Log
import io.ktor.client.features.cookies.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.ireader.core.LatestListing
import org.ireader.core.ParsedHttpSource
import org.ireader.core.PopularListing
import org.ireader.core.SearchListing
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import tachiyomi.source.Dependencies
import tachiyomi.source.model.*
import tachiyomix.annotations.Extension

//not working
@Extension
abstract class MtlNation(deps: Dependencies, private val clients: OkHttpClient) : ParsedHttpSource(deps) {

    override val name = "MtlNation"


    override val id: Long
        get() = 488631435
    override val baseUrl = "https://mtlnation.com"

    override val lang = "en"


    override fun getFilters(): FilterList {
        return listOf()
    }


    override fun getListings(): List<Listing> {
        return listOf(
            LatestListing(),
            PopularListing(),
            SearchListing()
        )
    }

    override fun fetchLatestEndpoint(page: Int): String? =
        "/novel/page/${page}/?m_orderby=latest"

    override fun fetchPopularEndpoint(page: Int): String? =
        "/novel/page/2/?m_orderby=views"

    override fun fetchSearchEndpoint(page: Int, query: String): String? =
        "/search/?searchkey=$query"


    fun headersBuilder() = Headers.Builder().apply {
        add(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
        )
        add("cache-control", "max-age=0")
        add("upapi", "true")
        add(
            "sec-ch-ua",
            "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"98\", \"Google Chrome\";v=\"98\""
        )
        add("sec-ch-ua-mobile", "?0")
        add("sec-ch-ua", "\"Windows\"")
        add("Upgrade-Insecure-Requests", "1")
        add(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        )
    }

    override val headers: Headers = headersBuilder().build()


    // popular
    override fun popularRequest(page: Int): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(baseUrl + fetchPopularEndpoint(page = page))
        }
    }

    override fun popularSelector() = "div.page-item-detail"

    override fun popularFromElement(element: Element): MangaInfo {

        val url = baseUrl + element.select("h3.h5 a").attr("href")
        val title = element.select("h3.h5 a").text()
        val thumbnailUrl = element.select("img").attr("src")
        return MangaInfo(key = url, title = title, cover = thumbnailUrl)
    }

    override fun popularNextPageSelector() = "a.last"


    // latest

    override fun latestRequest(page: Int): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(baseUrl + fetchLatestEndpoint(page)!!)
            headers { headers }

        }
    }

    override fun latestSelector(): String = popularSelector()


    override fun latestFromElement(element: Element): MangaInfo =
        popularFromElement(element = element)

    override fun latestNextPageSelector() = popularNextPageSelector()

    //Not configured from here

    override fun searchSelector() = "div.ul-list1 div.li-row"

    override fun searchFromElement(element: Element): MangaInfo {
        val title = element.select("div.txt a").attr("title")
        val url = baseUrl + element.select("div.txt a").attr("href")
        val thumbnailUrl = element.select("div.pic img").attr("src")
        return MangaInfo(key = url, title = title, cover = thumbnailUrl)
    }

    override fun searchNextPageSelector(): String? = null

    // manga details
    override fun detailParse(document: Document): MangaInfo {
        val title = document.select("div.m-desc h1.tit").text()
        val cover = document.select("div.m-book1 div.pic img").text()
        val link = baseUrl + document.select("div.cur div.wp a:nth-child(5)").attr("href")
        val authorBookSelector = document.select("div.right a.a1").attr("title")
        val description = document.select("div.inner p").eachText().joinToString("\n")
        //not sure why its not working.
        val category = document.select("[title=Genre]")
            .next()
            .text()
            .split(",")

        val status = document.select("[title=Status]")
            .next()
            .text()
            .replace("/[\t\n]/g", "")
            .handleStatus()




        return MangaInfo(
            title = title,
            cover = cover,
            description = description,
            author = authorBookSelector,
            genres = category,
            key = link,
            status = status
        )
    }

    private fun String.handleStatus(): Int {
        return when (this) {
            "OnGoing" -> MangaInfo.ONGOING
            "Complete" -> MangaInfo.COMPLETED
            else -> MangaInfo.ONGOING
        }

    }

    // chapters
    override fun chaptersRequest(book: MangaInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(book.key)
            headers { headers }
        }
    }

    override fun chaptersSelector() = "div.m-newest2 ul.ul-list5 li"

    override fun chapterFromElement(element: Element): ChapterInfo {
        val link = baseUrl + element.select("a").attr("href").substringAfter(baseUrl)
        val name = element.select("a").attr("title")

        return ChapterInfo(name = name, key = link)
    }

    fun uniqueChaptersRequest(book: MangaInfo, page: Int): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(
                book.key.replace("/${page - 1}.html", "").replace(".html", "")
                    .plus("/$page.html")
            )
            headers { headers }
        }
    }

    override suspend fun getChapterList(book: MangaInfo): List<ChapterInfo> {
        return kotlin.runCatching {
            return@runCatching withContext(Dispatchers.IO) {
                val page = client.get<String>(chaptersRequest(book = book))
                val maxPage = parseMaxPage(book)
                val list = mutableListOf<Deferred<List<ChapterInfo>>>()
                for (i in 1..maxPage) {
                    val pChapters = async {
                        chaptersParse(
                            client.get<String>(
                                uniqueChaptersRequest(
                                    book = book,
                                    page = i
                                )
                            ).parseHtml()
                        )
                    }
                    list.addAll(listOf(pChapters))
                }
                //  val request = client.get<String>(chaptersRequest(book = book))

                return@withContext list.awaitAll().flatten()
            }
        }.getOrThrow()
    }

    suspend fun parseMaxPage(book: MangaInfo): Int {
        val page = client.get<String>(chaptersRequest(book = book)).parseHtml()
        val maxPage = page.select("#indexselect option").eachText().size
        return maxPage
    }


    override fun pageContentParse(document: Document): List<String> {
        return document.select("div.txt h4,p").eachText()
    }

    override suspend fun getContents(chapter: ChapterInfo): List<String> {
        return pageContentParse(client.get<String>(contentRequest(chapter)).parseHtml())
    }


    override fun contentRequest(chapter: ChapterInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(chapter.key)
            headers { headers }
        }
    }


    override fun searchRequest(
        page: Int,
        query: String,
        filters: List<Filter<*>>,
    ): HttpRequestBuilder {
        return requestBuilder(baseUrl + fetchSearchEndpoint(page = page, query = query))
    }

    override suspend fun getSearch(query: String, filters: FilterList, page: Int): MangasPageInfo {
        return searchParse(client.get<String>(searchRequest(page, query, filters)).parseHtml())
    }


}