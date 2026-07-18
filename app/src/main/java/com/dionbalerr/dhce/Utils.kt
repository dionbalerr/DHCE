package com.dionbalerr.dhce

import android.util.Log

object ByteUtils
{
    fun createZeroByteArray(length: Int): ByteArray
    {
        var i = 0
        var zeroByteArray = byteArrayOf()
        while (i < length)
        {
            zeroByteArray += byteArrayOf(0x00)
            i += 1
        }
        Log.i("ZeroByteArray", "zeroByteArray with length $length: ${zeroByteArray.contentToString()}")
        return zeroByteArray
    }

}
