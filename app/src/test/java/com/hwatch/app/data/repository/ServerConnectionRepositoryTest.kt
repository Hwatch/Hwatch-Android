package com.hwatch.app.data.repository

import com.hwatch.app.data.model.DashboardDto
import com.hwatch.app.testutil.FakeHWatchApi
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ServerConnectionRepositoryTest {

    private fun repository(api: FakeHWatchApi): ServerConnectionRepository =
        ServerConnectionRepository(buildApi = { Result.success(api) })

    @Test
    fun invalidUrlFailsBeforeAnyRequest() = runBlocking {
        val result = repository(FakeHWatchApi()).testConnection("ftp://nas.local:8787", "zh-CN")
        assertEquals(ServerConnectionResult.Failure(ConnectionFailure.INVALID_URL), result)
    }

    @Test
    fun successReturnsNormalizedUrl() = runBlocking {
        val api = FakeHWatchApi { DashboardDto(timezone = "Asia/Shanghai") }
        val result = repository(api).testConnection("192.168.1.10:8787", "zh-CN")
        assertEquals(ServerConnectionResult.Success("http://192.168.1.10:8787/"), result)
    }

    @Test
    fun httpErrorMapsToHttp() = runBlocking {
        val api = FakeHWatchApi {
            throw HttpException(
                Response.error<Any>(
                    500,
                    """{"error":"x"}""".toResponseBody("application/json".toMediaType()),
                ),
            )
        }
        assertEquals(
            ServerConnectionResult.Failure(ConnectionFailure.HTTP),
            repository(api).testConnection("http://192.168.1.10:8787", "zh-CN"),
        )
    }

    @Test
    fun parseErrorMapsToParseWithSanitizedDetail() = runBlocking {
        val api = FakeHWatchApi { throw SerializationException("bad field external_links[0]") }
        val result = repository(api).testConnection("http://192.168.1.10:8787", "zh-CN")
        assertTrue(result is ServerConnectionResult.Failure)
        result as ServerConnectionResult.Failure
        assertEquals(ConnectionFailure.PARSE, result.kind)
        val detail = result.detail ?: error("expected sanitized detail")
        assertTrue(detail.startsWith("SerializationException"))
        assertTrue(!detail.contains("\n"))
    }

    @Test
    fun ioErrorMapsToUnreachable() = runBlocking {
        val api = FakeHWatchApi { throw IOException("no route") }
        assertEquals(
            ServerConnectionResult.Failure(ConnectionFailure.UNREACHABLE),
            repository(api).testConnection("http://192.168.1.10:8787", "zh-CN"),
        )
    }

    @Test
    fun timeoutMapsToTimeout() = runBlocking {
        val api = FakeHWatchApi { throw SocketTimeoutException("timeout") }
        assertEquals(
            ServerConnectionResult.Failure(ConnectionFailure.TIMEOUT),
            repository(api).testConnection("http://192.168.1.10:8787", "zh-CN"),
        )
    }
}
