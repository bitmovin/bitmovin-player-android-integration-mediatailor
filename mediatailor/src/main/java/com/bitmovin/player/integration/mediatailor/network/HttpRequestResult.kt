package com.bitmovin.player.integration.mediatailor.network

import com.bitmovin.player.integration.mediatailor.network.HttpRequestResult.Failure
import com.bitmovin.player.integration.mediatailor.network.HttpRequestResult.Success
import java.io.IOException

internal sealed class HttpRequestResult(val body: String?) {
    /**
     * Represents a successful HTTP request resulting in an HTTP response code between 200 and 299.
     */
    class Success(body: String? = null) : HttpRequestResult(body)

    /**
     * Represents a failed HTTP request that either resulted in an HTTP response code not between 200 and 299 or an
     * [IOException] that occurred during the request.
     */
    class Failure(body: String? = null, val exception: IOException? = null) :
        HttpRequestResult(body)
}

internal val HttpRequestResult.isSuccess: Boolean
    get() = this is Success

internal val HttpRequestResult.isFailure: Boolean
    get() = this is Failure
