package com.dionbalerr.dhce

import android.nfc.tech.IsoDep
import android.util.Log
import java.io.IOException
import java.util.Collections.emptyList
import kotlin.experimental.and

data class DiscoveredAid(val aid: String, val priority: Int)
data class Tlv(val tag: ByteArray, val length: Int, val value: ByteArray)

class NfcCardReader
{
    var masterList = mutableListOf<Tlv>()
    var masterAidList = mutableListOf<DiscoveredAid>()

    fun selectPpse(isoDep: IsoDep): ByteArray
    {
        var byteWithoutSw = byteArrayOf()

        masterList.clear()

        try
        {
            val responseBytes = isoDep.transceive(CommandApdu.SELECT_PPSE_COMMAND)
            printResponseApdu(responseBytes)
            val bool = (StatusWord.matchSw(responseBytes).contentEquals(StatusWord.SUCCESS))

            if (StatusWord.matchSw(responseBytes).contentEquals(StatusWord.SUCCESS))
            {
                byteWithoutSw = responseBytes.slice(0 until responseBytes.size - 2).toByteArray()
            }
            else return byteArrayOf()

        }
        catch (e: IOException)
        {
            e.printStackTrace()
        }

        return byteWithoutSw
    }

    fun parseBerTlvTags(byteArray: ByteArray, startIndex: Int, endIndex: Int): MutableList<Tlv>
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

            masterList.add(Tlv(tag, actualLength, value))

            if ((tag[0].toInt() and 0x20) == 0x20)
            {
                parseBerTlvTags((map["value"] as ByteArray), 0, (map["value"] as ByteArray).size)
            }

        }

        return masterList
    }

    fun parseAids(): MutableList<DiscoveredAid>
    {
        masterAidList.clear()

        val aidList = masterList.filter {
            it.tag.contentEquals(byteArrayOf(0x4F))
        }
        if (aidList.isEmpty()) return emptyList()

        val priorityList = masterList.filter {
            it.tag.contentEquals(byteArrayOf(0x87.toByte()))
        }
        val aidPriorityList = aidList.zip(priorityList)

        aidPriorityList.forEach { (aid, priority) ->
            val aidValue = aid.value.toHexString().uppercase()
            val priorityValue = priority.value[0].toInt()
            masterAidList.add(DiscoveredAid(aidValue, priorityValue))
        }

        masterAidList.forEach {
            Log.i("ByteTime", "AID: ${it.aid.uppercase()}, Priority: ${it.priority}")
        }

        return masterAidList
    }

    fun printResponseApdu(byteArray: ByteArray)
    {
        Log.d("RAPDU", byteArray.joinToString(" ")
        { "%02X".format(it) })
    }

    fun printMasterList()
    {
        masterList.forEach()
        {
            val tagByte = it.tag.toHexString().uppercase()
            val length = it.length
            val valueByte = it.value.toHexString().uppercase()
            Log.i("ByteTime",
                "Tag=${tagByte}, " +
                        "Len=${length}, " +
                        "Val=${valueByte}"
            )
        }
    }
}