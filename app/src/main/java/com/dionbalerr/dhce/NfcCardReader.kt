package com.dionbalerr.dhce

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import java.io.IOException

data class DiscoveredAid(val aid: String, val priority: Int)

class NfcCardReader
{
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
                return emptyList()
            }
            else return emptyList()

        }
        catch (e: IOException)
        {
            e.printStackTrace()
        }

        return discoverableAids
    }

    fun printResponseApdu(byteArray: ByteArray)
    {
        Log.d("RAPDU", byteArray.joinToString(" ")
        { "%02X".format(it) })
    }
}