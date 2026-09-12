package com.hwatch.app.ui.settings

import com.hwatch.app.data.model.SettingsDto
import com.hwatch.app.data.model.UpdateSettingsRequest
import com.hwatch.app.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMapperTest {

    private val sample = SettingsDto(
        httpProxy = "",
        downloadDir = "/media",
        ariaRpcUrl = "http://127.0.0.1:6800/jsonrpc",
        ariaSecret = "",
        checkTime = "04:00",
        timezone = "Asia/Shanghai",
        plexLanguage = "zh-CN",
        maxConcurrent = 2,
        connectionsPerDownload = 4,
        downloadLimitKbps = 0,
        minFreeBytes = 5L * SettingsMapper.BYTES_PER_GB,
        requireCreatorMatch = true,
        excludeTitleKeywords = "compilation",
        maxDurationSeconds = 0,
    )

    @Test
    fun toDraftMapsFieldsIncludingGb() {
        val draft = SettingsMapper.toDraft(sample)
        assertEquals("Asia/Shanghai", draft.timezone)
        assertEquals("2", draft.maxConcurrent)
        assertEquals("5", draft.minFreeGb)
        assertEquals("0", draft.downloadLimitKbps)
        assertTrue(draft.requireCreatorMatch)
    }

    @Test
    fun noChangesProducesEmptyRequest() {
        val request = SettingsMapper.buildUpdateRequest(sample, SettingsMapper.toDraft(sample))
        assertEquals(UpdateSettingsRequest(), request)
        assertFalse(SettingsMapper.hasChanges(sample, SettingsMapper.toDraft(sample)))
    }

    @Test
    fun onlyChangedFieldsAreIncluded() {
        val draft = SettingsMapper.toDraft(sample).copy(maxConcurrent = "4")
        val request = SettingsMapper.buildUpdateRequest(sample, draft)
        assertEquals(4, request.maxConcurrent)
        assertNull(request.httpProxy)
        assertNull(request.timezone)
        assertNull(request.minFreeBytes)
        assertNull(request.ariaSecret)
    }

    @Test
    fun downloadLimitZeroIsLegalAndChangeable() {
        val original = sample.copy(downloadLimitKbps = 100)
        val draft = SettingsMapper.toDraft(original).copy(downloadLimitKbps = "0")
        assertTrue(SettingsMapper.invalidFields(draft).isEmpty())
        assertEquals(0, SettingsMapper.buildUpdateRequest(original, draft).downloadLimitKbps)
    }

    @Test
    fun invalidNumericFieldsAreReported() {
        val draft = SettingsMapper.toDraft(sample).copy(
            maxConcurrent = "abc",
            connectionsPerDownload = "-1",
            downloadLimitKbps = "x",
            minFreeGb = "y",
            maxDurationSeconds = "z",
        )
        val invalid = SettingsMapper.invalidFields(draft)
        assertTrue(SettingsField.MAX_CONCURRENT in invalid)
        assertTrue(SettingsField.CONNECTIONS_PER_DOWNLOAD in invalid)
        assertTrue(SettingsField.DOWNLOAD_LIMIT_KBPS in invalid)
        assertTrue(SettingsField.MIN_FREE_GB in invalid)
        assertTrue(SettingsField.MAX_DURATION_SECONDS in invalid)
    }

    @Test
    fun timezoneAndCheckTimeValidation() {
        val bad = SettingsMapper.toDraft(sample).copy(timezone = "Bad/Zone", checkTime = "25:99")
        val invalid = SettingsMapper.invalidFields(bad)
        assertTrue(SettingsField.TIMEZONE in invalid)
        assertTrue(SettingsField.CHECK_TIME in invalid)

        val good = SettingsMapper.toDraft(sample).copy(timezone = "UTC", checkTime = "23:59")
        assertTrue(SettingsMapper.invalidFields(good).isEmpty())
    }

    @Test
    fun gbToBytesIsLongSafe() {
        assertEquals(SettingsMapper.BYTES_PER_GB, SettingsMapper.gbToBytes("1"))
        assertEquals(10 * SettingsMapper.BYTES_PER_GB, SettingsMapper.gbToBytes("10"))
        assertEquals(0L, SettingsMapper.gbToBytes("0"))
        assertNull(SettingsMapper.gbToBytes("abc"))
        assertNull(SettingsMapper.gbToBytes("-1"))
        assertNull(SettingsMapper.gbToBytes("999999999999999999999"))
    }

    @Test
    fun bytesToGbString() {
        assertEquals("5", SettingsMapper.bytesToGbString(5 * SettingsMapper.BYTES_PER_GB))
        assertEquals("0", SettingsMapper.bytesToGbString(0))
    }

    @Test
    fun updateSettingsRequestOnlyEncodesChangedField() {
        val json = NetworkModule.createJson()
        val encoded = json.encodeToString(
            UpdateSettingsRequest.serializer(),
            UpdateSettingsRequest(maxConcurrent = 4),
        )
        assertEquals("{\"max_concurrent\":4}", encoded)
    }

    @Test
    fun ariaSecretUnchangedIsNotSent() {
        val draft = SettingsMapper.toDraft(sample).copy(timezone = "UTC")
        val request = SettingsMapper.buildUpdateRequest(sample, draft)
        assertNull(request.ariaSecret)
        assertEquals("UTC", request.timezone)
    }
}
