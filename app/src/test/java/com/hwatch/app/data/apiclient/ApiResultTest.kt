package com.hwatch.app.data.apiclient

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiResultTest {

    @Test
    fun success_mapsToSuccess() = runBlocking {
        val result = safeApiCall { 42 }
        assertTrue(result is ApiResult.Success)
        assertEquals(42, (result as ApiResult.Success).data)
    }

    @Test
    fun httpError_mapsCodeAndMessage() = runBlocking {
        val response = errorResponse(404, "{\"error\":\"任务不存在\"}")
        val result = safeApiCall<Any> { throw HttpException(response) }
        assertTrue(result is ApiResult.HttpError)
        result as ApiResult.HttpError
        assertEquals(404, result.code)
        assertEquals("任务不存在", result.message)
    }

    @Test
    fun httpError_nonJsonBodyMessageNull() = runBlocking {
        val response = errorResponse(500, "<html>oops</html>")
        val result = safeApiCall<Any> { throw HttpException(response) }
        assertTrue(result is ApiResult.HttpError)
        assertEquals(500, (result as ApiResult.HttpError).code)
        assertNull(result.message)
    }

    @Test
    fun networkError_mapsIOException() = runBlocking {
        val result = safeApiCall<Any> { throw IOException("nas offline") }
        assertTrue(result is ApiResult.NetworkError)
        assertEquals("nas offline", (result as ApiResult.NetworkError).cause.message)
    }

    @Test
    fun parseError_mapsSerializationException() = runBlocking {
        val result = safeApiCall<Any> { throw SerializationException("bad json") }
        assertTrue(result is ApiResult.ParseError)
    }

    @Test
    fun cancellationExceptionIsRethrown() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                safeApiCall<Any> { throw CancellationException("cancel") }
            }
        }
    }

    @Test
    fun extractErrorMessage_parsesErrorBody() {
        val response = errorResponse(400, "{\"error\":\"参数错误\"}")
        assertEquals("参数错误", extractErrorMessage(HttpException(response)))
    }

    @Test
    fun extractErrorMessage_unparseableReturnsNull() {
        val response = errorResponse(400, "{not json")
        assertNull(extractErrorMessage(HttpException(response)))
    }

    @Test
    fun extractErrorMessage_readsErrorWithStableCode() {
        val response = errorResponse(404, "{\"code\":\"not_found\",\"error\":\"missing\"}")
        assertEquals("missing", extractErrorMessage(HttpException(response)))
    }

    private fun errorResponse(code: Int, body: String): retrofit2.Response<Any> {
        return Response.error(code, body.toResponseBody("application/json".toMediaType()))
    }
}