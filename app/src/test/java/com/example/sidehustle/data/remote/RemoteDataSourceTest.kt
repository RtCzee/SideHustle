package com.example.sidehustle.data.remote

import com.example.sidehustle.network.SideHustleApi
import java.io.IOException
import java.lang.reflect.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

class RemoteDataSourceTest {
    // Transport is never invoked: each test supplies the operation passed to safeApiCall.
    private val unusedApi = Proxy.newProxyInstance(
        SideHustleApi::class.java.classLoader, arrayOf(SideHustleApi::class.java),
    ) { _, _, _ -> error("Unexpected transport call") } as SideHustleApi
    private val remote = RemoteDataSource(unusedApi)

    @Test fun `unreachable host and IO failures become network errors`() = runBlocking {
        listOf(UnknownHostException(), IOException()).forEach { failure ->
            val result = remote.safeApiCall<String> { throw failure } as ApiResult.Error
            assertEquals(ErrorKind.NETWORK, result.kind)
        }
    }

    @Test fun `timeout has its own kind and unknown failures have safe messages`() = runBlocking {
        val timeout = remote.safeApiCall<String> { throw SocketTimeoutException() } as ApiResult.Error
        assertEquals(ErrorKind.TIMEOUT, timeout.kind)
        val unknown = remote.safeApiCall<String> { error("private diagnostic") } as ApiResult.Error
        assertEquals(ErrorKind.UNKNOWN, unknown.kind)
        assertFalse(unknown.message.contains("private diagnostic"))
    }

    @Test fun `view cancellation propagates rather than becoming a visible error`() = runBlocking {
        val cancellation = CancellationException("View destroyed")
        try {
            remote.safeApiCall<String> { throw cancellation }
            fail("Cancellation was swallowed")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test fun `failed operation can be invoked again successfully`() = runBlocking {
        var requests = 0
        val operation: suspend () -> String = {
            requests++
            if (requests == 1) throw IOException()
            "Recovered"
        }
        assertTrue(remote.safeApiCall(operation) is ApiResult.Error)
        assertEquals(ApiResult.Success("Recovered"), remote.safeApiCall(operation))
        assertEquals(2, requests)
    }

    @Test fun `late transport failures cannot resume a cancelled screen`() = runBlocking {
        var delivered = false
        val request = launch {
            remote.safeApiCall<String> {
                currentCoroutineContext().cancel()
                throw IOException("Request finished after leaving the screen")
            }
            delivered = true
        }
        request.join()
        assertTrue(request.isCancelled)
        assertFalse(delivered)
    }
}
