package com.hwatch.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun formatFileSize_zero() {
        assertEquals("0 B", ByteFormatter.formatFileSize(0))
    }

    @Test
    fun formatFileSize_belowKib() {
        assertEquals("512 B", ByteFormatter.formatFileSize(512))
    }

    @Test
    fun formatFileSize_kib() {
        assertEquals("1.0 KB", ByteFormatter.formatFileSize(1024))
    }

    @Test
    fun formatFileSize_mib() {
        assertEquals("1.0 MB", ByteFormatter.formatFileSize(1024L * 1024))
    }

    @Test
    fun formatFileSize_gib() {
        assertEquals("1.0 GB", ByteFormatter.formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun formatFileSize_tib() {
        assertEquals("1.0 TB", ByteFormatter.formatFileSize(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun formatFileSize_decimal() {
        assertEquals("899.2 GB", ByteFormatter.formatFileSize(965_554_995_200L))
        assertEquals("120.6 KB", ByteFormatter.formatFileSize(123_456L))
    }

    @Test
    fun formatDownloadSpeed_zero() {
        assertEquals("0 B/s", ByteFormatter.formatDownloadSpeed(0))
    }

    @Test
    fun formatDownloadSpeed_kibs() {
        assertEquals("1.0 KB/s", ByteFormatter.formatDownloadSpeed(1024))
        assertEquals("1.5 KB/s", ByteFormatter.formatDownloadSpeed(1536))
    }

    @Test
    fun formatDownloadSpeed_mibs() {
        assertEquals("1.0 MB/s", ByteFormatter.formatDownloadSpeed(1024L * 1024))
        assertEquals("2.3 MB/s", ByteFormatter.formatDownloadSpeed(2_412_000L))
    }
}