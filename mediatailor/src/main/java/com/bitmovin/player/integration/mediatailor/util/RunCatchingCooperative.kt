package com.bitmovin.player.integration.mediatailor.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Calls the specified function [block] and returns its encapsulated result if
 * invocation was successful, catching any [Throwable] exception (excluding [CancellationException])
 * that was thrown from the block function execution and encapsulating it as a failure.
 *
 * [CancellationException] is ignored to allow cooperative cancellation of the coroutine.
 */
internal inline fun <R> runCatchingCooperative(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
