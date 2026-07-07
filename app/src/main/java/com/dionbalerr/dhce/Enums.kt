package com.dionbalerr.dhce

object StatusWord
{
    val SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
    val STATUS_FAILED = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    val FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x83.toByte())
    val COMMAND_NOT_ALLOWED = byteArrayOf(0x69.toByte(), 0x86.toByte())
    val WRONG_LENGTH = byteArrayOf(0x67.toByte(), 0x00.toByte())
    val CONDITIONS_NOT_SATISFIED = byteArrayOf(0x69.toByte(), 0x85.toByte())
    val INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
    val CLA_NOT_SUPPORTED = byteArrayOf(0x6E.toByte(), 0x00.toByte())

    val map = mapOf(
        "9000" to SUCCESS,
        "6A82" to STATUS_FAILED)

    fun matchSw(response: ByteArray): ByteArray
    {
        if (response.size < 2) return STATUS_FAILED

        val sw = response.takeLast(2).joinToString("") { "%02X".format(it) }

        return map[sw] ?: STATUS_FAILED
    }
}

object CommandApdu
{
    val SELECT_PPSE_COMMAND = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x0E, 0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31, 0x00)

}