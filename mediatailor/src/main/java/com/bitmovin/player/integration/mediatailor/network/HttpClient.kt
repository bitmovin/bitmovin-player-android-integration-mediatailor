package com.bitmovin.player.integration.mediatailor.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

internal interface HttpClient {
    suspend fun get(url: String): HttpRequestResult
    suspend fun post(url: String): HttpRequestResult
}

internal class DefaultHttpClient : HttpClient {
    override suspend fun get(url: String): HttpRequestResult = request(url) { urlConnection ->
        urlConnection.requestMethod = "GET"
    }

    override suspend fun post(url: String): HttpRequestResult = request(url) { urlConnection ->
        urlConnection.requestMethod = "POST"
    }

    private suspend fun request(
        url: String,
        configureHttpURLConnection: (HttpURLConnection) -> Unit,
    ): HttpRequestResult = withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null

        try {
            urlConnection = URL(url).openConnection() as HttpURLConnection
            configureHttpURLConnection(urlConnection)

            if (urlConnection.responseCode.isSuccessfulHttpResponse()) {
                HttpRequestResult.Success(body = urlConnection.inputStream.readUtf8String())
            } else {
                HttpRequestResult.Failure(body = urlConnection.errorStream?.readUtf8String())
            }
        } catch (exception: IOException) {
            HttpRequestResult.Failure(exception = exception)
        } finally {
            urlConnection?.disconnect()
        }
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
