package com.dionbalerr.dhce

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import java.io.IOException
import kotlin.experimental.and

data class DiscoveredAid(val aid: String, val priority: Int)

class NfcCardReader
{
    var masterList = mutableListOf<MutableMap<String, Any>>()
    fun readCard(tag: Tag): List<DiscoveredAid>
    {
        val isoDep = IsoDep.get(tag) ?: return emptyList()
        val discoverableAids = mutableListOf<DiscoveredAid>()

        try
        {
            isoDep.connect()
            isoDep.timeout = 500

            val responseBytes = isoDep.transceive(CommandApdu.SELECT_PPSE_COMMAND)
            printResponseApdu(responseBytes)
            if (StatusWord.matchSw(responseBytes).contentEquals(StatusWord.SUCCESS))
            {
                val byteWithoutSw = responseBytes.slice(0 until responseBytes.size - 2).toByteArray()
                parseBerTlvTags(byteWithoutSw, 0, byteWithoutSw.size)
            }
            else return emptyList()

            masterList.forEach()
            {
                val tagByte = (it["tag"] as ByteArray).toHexString().uppercase()
                val length = it["length"]
                val valueByte = (it["value"] as ByteArray).toHexString().uppercase()
                Log.i("ByteTime",
                    "Tag=${tagByte}, " +
                            "Len=${length}, " +
                            "Val=${valueByte}"
                )
            }

        }
        catch (e: IOException)
        {
            e.printStackTrace()
        }

        return discoverableAids
    }

    private fun parseBerTlvTags(byteArray: ByteArray, startIndex: Int, endIndex: Int): MutableList<MutableMap<String, Any>>
    {
        var pointer = startIndex

        while (pointer < endIndex)
        {
            val map = mutableMapOf<String, Any>()
            val tagStartIndex = pointer

            // Check tags here
            val firstByteTag = byteArray[pointer]

            if ((firstByteTag and 0x1F) == 0x1F.toByte())
            {
                pointer += 1
            }
            val tag = byteArray.sliceArray(tagStartIndex .. pointer)
            map["tag"] = tag
            pointer += 1

            // Check length
            val actualLength = byteArray[pointer].toInt() and 0xFF
            map["length"] = actualLength
            pointer += 1

            // Check value
            val value = byteArray.sliceArray(pointer until pointer + actualLength)
            map["value"] = value
            pointer += actualLength

            masterList.add(map)

            if ((tag[0].toInt() and 0x20) == 0x20)
            {
                parseBerTlvTags((map["value"] as ByteArray), 0, (map["value"] as ByteArray).size)
            }

        }

        return masterList
    }

    fun printResponseApdu(byteArray: ByteArray)
    {
        Log.d("RAPDU", byteArray.joinToString(" ")
        { "%02X".format(it) })
    }
}