package com.dionbalerr.dhce

import androidx.compose.ui.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dionbalerr.dhce.ui.NfcScannerScreen
import com.dionbalerr.dhce.ui.theme.DHCETheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null
    private val cardReader = NfcCardReader()
    private var statusText by mutableStateOf("Status: Ready to scan")
    private var isScanning by mutableStateOf(false)
    private var discoveredAids by mutableStateOf<List<DiscoveredAid>>(emptyList())

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        enableEdgeToEdge()
        setContent()
        {
            DHCETheme()
            {
                Scaffold(
                    topBar =
                        {
                            TopAppBar(
                                title = { Text(text = "DHCE") },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Green,
                                    titleContentColor = Color.Black
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                    modifier = Modifier.fillMaxSize()
                )
                { innerPadding ->
                    NfcScannerScreen(
                        statusText = statusText,
                        isScanning = isScanning,
                        discoveredAids = discoveredAids,
                        onScanStart = { startNfcScanningSession() },
                        onScanStop = { isTimeout -> stopNfcScanningSession(isTimeout)},
//                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?)
    {
        if (tag != null)
        {
            val results = cardReader.readCard(tag)

            nfcAdapter?.disableReaderMode(this)

            runOnUiThread()
            {
                isScanning = false
                discoveredAids = results
                statusText =
                    if (results.isNotEmpty()) {
                        "Status: Read complete!"
                    } else {
                        "Status: Card detected\n" +
                                "$discoveredAids"
                    }
            }
        }
    }

    override fun onPause()
    {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        isScanning = false
    }

    private fun startNfcScanningSession()
    {
        if (nfcAdapter == null)
        {
            statusText = "Error: Device does not support NFC."
            return
        }

        if (!nfcAdapter!!.isEnabled)
        {
            statusText = "Error: NFC is switched off in system settings."
            return
        }

        discoveredAids = emptyList()
        isScanning = true
        statusText = "Status: Looking for card..."

        val options = Bundle()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            options
        )
    }

    private fun stopNfcScanningSession(isTimeout: Boolean)
    {
        isScanning = false
        statusText = if (isTimeout) "Timeout" else "Canceled"
        nfcAdapter?.disableReaderMode(this)
    }
}