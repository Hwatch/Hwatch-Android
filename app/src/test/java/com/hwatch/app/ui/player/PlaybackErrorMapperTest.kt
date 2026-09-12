package com.hwatch.app.ui.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorMapperTest {

    @Test
    fun networkErrorsMapToNetwork() {
        assertEquals(
            PlaybackErrorKind.NETWORK,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, null),
        )
        assertEquals(
            PlaybackErrorKind.NETWORK,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, null),
        )
        assertEquals(
            PlaybackErrorKind.NETWORK,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_UNSPECIFIED, null),
        )
    }

    @Test
    fun httpStatusMapsToNotFoundOrServer() {
        assertEquals(
            PlaybackErrorKind.NOT_FOUND,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 404),
        )
        assertEquals(
            PlaybackErrorKind.SERVER,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 500),
        )
        assertEquals(
            PlaybackErrorKind.SERVER,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, null),
        )
    }

    @Test
    fun fileNotFoundMapsToNotFound() {
        assertEquals(
            PlaybackErrorKind.NOT_FOUND,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, null),
        )
    }

    @Test
    fun unsupportedParsingMapsToUnsupported() {
        assertEquals(
            PlaybackErrorKind.UNSUPPORTED,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, null),
        )
        assertEquals(
            PlaybackErrorKind.UNSUPPORTED,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED, null),
        )
    }

    @Test
    fun decoderErrorsMapToDecoder() {
        assertEquals(
            PlaybackErrorKind.DECODER,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, null),
        )
        assertEquals(
            PlaybackErrorKind.DECODER,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_DECODING_FAILED, null),
        )
    }

    @Test
    fun unknownCodeMapsToUnknown() {
        assertEquals(
            PlaybackErrorKind.UNKNOWN,
            PlaybackErrorMapper.map(PlaybackException.ERROR_CODE_UNSPECIFIED, null),
        )
    }

    @Test
    fun httpStatusExtractionIgnoresUnrelatedCause() {
        org.junit.Assert.assertNull(PlaybackErrorMapper.httpStatusFrom(null))
        org.junit.Assert.assertNull(PlaybackErrorMapper.httpStatusFrom(java.io.IOException("boom")))
    }
}
