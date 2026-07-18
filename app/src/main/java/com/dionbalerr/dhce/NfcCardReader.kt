package com.dionbalerr.dhce

import android.nfc.tech.IsoDep
import android.util.Log
import java.io.IOException
import java.time.LocalDate
import java.util.Collections.emptyList
import kotlin.experimental.and

data class DiscoveredAid(val aid: String, val priority: Int)
data class DiscoveredAid(val aid: ByteArray, val priority: Int)
data class Tlv(val tag: ByteArray, val length: Int, val value: ByteArray)

class NfcCardReader
{
    var masterPpseList = mutableListOf<Tlv>()
    var masterAidList = mutableListOf<DiscoveredAid>()
    var listResponseSelectAid = mutableListOf<Tlv>()
    var listResponsePdol = mutableListOf<Tlv>()
    var listResponseGpo = mutableListOf<Tlv>()
    var listResponseAfl = mutableListOf<Tlv>()
    var listReadRecordCApdu = mutableListOf<ByteArray>()

    var nameOfList = "SELECT_PPSE"

    fun selectPpse(isoDep: IsoDep): ByteArray
    {
        nameOfList = "SELECT_PPSE"
        var byteWithoutSw = byteArrayOf()

        masterPpseList.clear()

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
        val currentList: MutableList<Tlv> =
            when (nameOfList)
            {
                "SELECT_AID" -> listResponseSelectAid
                "APPLICATION_FILE_LOCATOR" -> listResponseAfl
                "GET_PROCESSING_OPTIONS" -> listResponseGpo
                else -> masterPpseList
            }

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

            currentList.add(Tlv(tag, actualLength, value))

            if ((tag[0].toInt() and 0x20) == 0x20)
            {
                parseBerTlvTags((map["value"] as ByteArray), 0, (map["value"] as ByteArray).size)
            }

        }

        return currentList
    }

    fun parseAids(): MutableList<DiscoveredAid>
    {
        masterAidList.clear()

        val aidList = masterPpseList.filter {
            it.tag.contentEquals(byteArrayOf(0x4F))
        }
        if (aidList.isEmpty()) return emptyList()

        val priorityList = masterPpseList.filter {
            it.tag.contentEquals(byteArrayOf(0x87.toByte()))
        }
        val aidPriorityList = aidList.zip(priorityList)

        aidPriorityList.forEach { (aid, priority) ->
            val aidValue = aid.value
            val priorityValue = priority.value[0].toInt()
            masterAidList.add(DiscoveredAid(aidValue, priorityValue))
        }

        masterAidList.forEach {
            Log.i("ByteTime", "AID: ${it.aid.joinToString("") { "%02X".format(it) }}, Priority: ${it.priority}")
        }

        return masterAidList
    }

    fun selectAid(isoDep: IsoDep): ByteArray
    {
        nameOfList = "SELECT_AID"
        var byteWithoutSw = byteArrayOf()

        try
        {
            val selectedAid = masterAidList.first().aid
//            printResponseApdu(selectedAid)
            val selectAidCommand = CommandApdu.SELECT_AID_HEADER + selectedAid.size.toByte() + selectedAid + byteArrayOf(0x00)
            printResponseApdu(selectAidCommand)

            val responseSelectAid = isoDep.transceive(selectAidCommand)
            printResponseApdu(responseSelectAid)

            if (StatusWord.matchSw(responseSelectAid).contentEquals(StatusWord.SUCCESS))
            {
                byteWithoutSw = responseSelectAid.slice(0 until responseSelectAid.size - 2).toByteArray()
            }
            else return byteArrayOf()

            return byteWithoutSw
        }
        catch (e: IOException)
        {
            e.printStackTrace()
        }
        return byteWithoutSw
    }

    fun parsePdol(): MutableList<Tlv>
    {
        nameOfList = "PDOL"

        val pdolList = listResponseSelectAid.filter {
            it.tag.contentEquals(byteArrayOf(0x9f.toByte(), 0x38))
        }
        pdolList.forEach { Log.i("PDOLLIST", "tag=${it.tag.toHexString()}, length=${it.length}, value=${it.value.toHexString().uppercase()}") }

        if (pdolList.isEmpty()) return emptyList()

//        Log.i("PDOL", "pdolList[0].value: ${pdolList[0].value.joinToString("") { "%02X".format(it) }}")
//        Log.i("PDOL", "pdolList[0].value.size: ${pdolList[0].value.size}")
        val resPdol = parsePdolTags(pdolList[0].value)

        return resPdol
    }

    // Only TL no V
    fun parsePdolTags(byteArray: ByteArray): MutableList<Tlv>
    {
        listResponsePdol.clear()

        var pointer = 0
        val endIndex = byteArray.size

        while (pointer < endIndex)
        {
            val map = mutableMapOf<String, Any>()
            val tagStartIndex = pointer

            val firstByteTag = byteArray[pointer]

            if ((firstByteTag and 0x1F) == 0x1F.toByte())
            {
                pointer += 1
            }
            val tag = byteArray.sliceArray(tagStartIndex .. pointer)
            map["tag"] = tag
            pointer += 1

            val actualLength = byteArray[pointer].toInt() and 0xFF
            map["length"] = actualLength
            pointer += 1

            listResponsePdol.add(Tlv(tag, actualLength, byteArrayOf()))
        }

        return listResponsePdol
    }

    fun getProcessingOptions(isoDep: IsoDep): ByteArray
    {
        var byteWithoutSw = byteArrayOf()

        try
        {
            val createTtq = CommandApdu.VISA_TTQ
            val createAmount = CommandApdu.DEFAULT_AMOUNT
//            val createCashback = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            val createCashback = ByteUtils.createZeroByteArray(5)
            val createTcc = CommandApdu.COUNTRY_MALAYSIA
            val createTvr = ByteUtils.createZeroByteArray(5)
            val createCurrencyCode = CommandApdu.COUNTRY_MALAYSIA
            val date = LocalDate.now()
            val yymmdd = "%02d%02d%02d".format(date.year % 100, date.monthValue, date.dayOfMonth)
            val createDate = yymmdd.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val createTransactionType = byteArrayOf(0x00)
            val createUnpredictableNumber = byteArrayOf(0xA1.toByte(), 0xB2.toByte(), 0xC3.toByte(), 0xD4.toByte())

            val commandDataPayload = createTtq + createAmount + createCashback + createTcc + createTvr + createCurrencyCode + createDate + createTransactionType + createUnpredictableNumber
            val commandDataTlv = byteArrayOf(0x83.toByte(), commandDataPayload.size.toByte(), *commandDataPayload)

            val getProcessingOptionsCommand = CommandApdu.GPO_HEADER + commandDataTlv.size.toByte() + commandDataTlv + byteArrayOf(0x00)
            printResponseApdu(getProcessingOptionsCommand)

            val responseGetProcessingOptions = isoDep.transceive(getProcessingOptionsCommand)
            printResponseApdu(responseGetProcessingOptions)

            if (StatusWord.matchSw(responseGetProcessingOptions).contentEquals(StatusWord.SUCCESS))
            {
                byteWithoutSw = responseGetProcessingOptions.slice(0 until responseGetProcessingOptions.size - 2).toByteArray()
            }
            else return byteArrayOf()

//            printResponseApdu(byteWithoutSw)
            return byteWithoutSw
        }
        catch (e: IOException)
        {
            e.printStackTrace()
        }

        return byteWithoutSw
    }

    fun parseGpoTags(responseGpo: ByteArray)
    {
        nameOfList = "APPLICATION_FILE_LOCATOR"

        // Format 1: compact, not tlv, tl only
        if (responseGpo[0] == 0x80.toByte())
        {

        }
        // Format 2: TLV
        else
        {
            val afl = parseBerTlvTags(responseGpo, 0, responseGpo.size)
            printTlvList(afl)
        }
    }

    fun printResponseApdu(byteArray: ByteArray)
    {
        Log.d("RAPDU", byteArray.joinToString(" ")
        { "%02X".format(it) })
    }

    fun printTlvList(tlvList: MutableList<Tlv>)
    {
        tlvList.forEach()
        {
            val tagByte = it.tag.toHexString().uppercase()
            val length = it.length
            val valueByte = it.value.toHexString().uppercase()
            Log.i("$tlvList",
                "Tag=${tagByte}, " +
                        "Len=${length}, " +
                        "Val=${valueByte}"
            )
        }
    }

    fun printMasterList()
    {
        masterPpseList.forEach()
        {
            val tagByte = it.tag.toHexString().uppercase()
            val length = it.length
            val valueByte = it.value.toHexString().uppercase()
            Log.i("printMasterList",
                "Tag=${tagByte}, " +
                        "Len=${length}, " +
                        "Val=${valueByte}"
            )
        }
    }

    fun printSelectAid()
    {
        listResponseSelectAid.forEach()
        {
            val tagByte = it.tag.toHexString().uppercase()
            val length = it.length
            val valueByte = it.value.toHexString().uppercase()
            Log.i("printSelectAid",
                "Tag=${tagByte}, " +
                        "Len=${length}, " +
                        "Val=${valueByte}"
            )
        }
    }

    fun printPdolList()
    {
        listResponsePdol.forEach()
        {
            val tagByte = it.tag.toHexString().uppercase()
            val length = it.length
            val valueByte = it.value.toHexString().uppercase()
            Log.i("printPdolList",
                "Tag=${tagByte}, " +
                        "Len=${length}, " +
                        "Val=${valueByte}"
            )
        }
    }
}