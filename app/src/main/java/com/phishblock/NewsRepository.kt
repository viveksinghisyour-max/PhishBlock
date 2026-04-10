package com.phishblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class NewsItem(val title: String, val link: String, val pubDate: String)

object NewsRepository {
    suspend fun fetchNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            val url = "https://krebsonsecurity.com/feed/"
            val doc = Jsoup.connect(url).get()
            val items = doc.select("item")
            items.take(10).mapNotNull { item ->
                val title = item.select("title").text()
                val link = item.select("link").text()
                val pubDate = item.select("pubDate").text()
                if (title.isNotBlank()) NewsItem(title, link, pubDate) else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}