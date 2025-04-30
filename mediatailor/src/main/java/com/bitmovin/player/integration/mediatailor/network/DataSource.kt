package com.bitmovin.player.integration.mediatailor.network

import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingSession
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

internal interface DataSource {
    suspend fun post(): String?
}

internal class DefaultDataSource(
    private val url: String
) : DataSource {
    override suspend fun post(): String? = withContext(Dispatchers.IO) {
        var result: String?
        var urlConnection: HttpURLConnection? = null

        try {
            urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"

            val responseCode = urlConnection.responseCode
            result = if (responseCode.isSuccessfulHttpResponse()) {
                urlConnection.inputStream.readUtf8String()
            } else {
                null
            }
        } catch (exception: IOException) {
            result = null
        } finally {
            urlConnection?.disconnect()
        }
        return@withContext result
    }
}

private fun Int.isSuccessfulHttpResponse() = this in 200..299

private suspend fun InputStream.readUtf8String() = use {
    closeOnCancel {
        String(it.readBytes(), Charsets.UTF_8)
    }
}

private suspend fun <T : Closeable, R> T.closeOnCancel(block: suspend (T) -> R): R =
    coroutineScope {
        try {
            async { block(this@closeOnCancel) }.await()
        } catch (e: CancellationException) {
            close()
            throw e
        }
    }
