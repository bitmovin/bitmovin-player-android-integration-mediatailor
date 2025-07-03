package util

import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.HttpRequestResult

internal class TestHttpClient : HttpClient {
    val getRequests = mutableListOf<String>()
    val postRequests = mutableListOf<String>()

    override suspend fun get(url: String): HttpRequestResult {
        getRequests.add(url)
        return HttpRequestResult.Success("Mock response")
    }

    override suspend fun post(url: String, body: Map<String, Any>): HttpRequestResult {
        postRequests.add(url)
        return HttpRequestResult.Success("Mock response")
    }

    fun clear() {
        getRequests.clear()
        postRequests.clear()
    }
}
