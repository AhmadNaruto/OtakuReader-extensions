package org.ireader.core

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.ireader.core_api.source.Dependencies
import org.ireader.core_api.source.HttpSource
import org.ireader.core_api.source.model.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.security.MessageDigest

/** Taken from https://tachiyomi.org/ **/
abstract class ParsedHttpSource(private val dependencies: Dependencies) : HttpSource(dependencies) {


    override val id: Long by lazy {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }
                .reduce(Long::or) and Long.MAX_VALUE
    }


    open fun HttpRequestBuilder.headersBuilder() {
        headers {
            append(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
            append(HttpHeaders.CacheControl, "max-age=0")
        }
    }


    fun requestBuilder(
            url: String,
            block: HeadersBuilder.() -> Unit = {
                append(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
                append(HttpHeaders.CacheControl, "max-age=0")
            }
    ): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(url)
            HeadersBuilder().apply(block)
        }
    }


    protected open fun detailRequest(manga: MangaInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(manga.key)
            headersBuilder()

        }
    }

    override suspend fun getMangaDetails(manga: MangaInfo): MangaInfo {
        return detailParse(client.get(detailRequest(manga)).asJsoup())
    }

    open fun chaptersRequest(book: MangaInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(book.key)
            headersBuilder()
        }
    }

    override suspend fun getPageList(chapter: ChapterInfo): List<Page> {
        return getContents(chapter).map { Text(it) }
    }

    open suspend fun getContents(chapter: ChapterInfo): List<String> {
        return pageContentParse(client.get(contentRequest(chapter)).asJsoup())
    }

    open fun contentRequest(chapter: ChapterInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(chapter.key)
            headersBuilder()
        }
    }

    abstract fun chapterFromElement(element: Element): ChapterInfo


    fun bookListParse(document: Document, elementSelector: String, nextPageSelector: String?, parser: (element: Element) -> MangaInfo): MangasPageInfo {
        val books = document.select(elementSelector).map { element ->
            parser(element)
        }

        val hasNextPage = nextPageSelector?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPageInfo(books, hasNextPage)
    }

    abstract fun chaptersSelector(): String

    open fun chaptersParse(document: Document): List<ChapterInfo> {
        return document.select(chaptersSelector()).map { chapterFromElement(it) }
    }


    abstract fun pageContentParse(
            document: Document,
    ): List<String>


    abstract fun detailParse(document: Document): MangaInfo
}