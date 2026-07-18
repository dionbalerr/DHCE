package com.dionbalerr.dhce.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dionbalerr.dhce.DiscoveredAid
import kotlinx.coroutines.delay
import kotlin.collections.isNotEmpty

@Composable
fun NfcScannerScreen(
    statusText: String,
    isScanning: Boolean,
    discoveredAids: List<DiscoveredAid>,
    byteArray: ByteArray,
    onScanStart: () -> Unit,
    onScanStop: (isTimeout: Boolean) -> Unit,
    modifier: Modifier = Modifier
)
{
    var secondsLeft by remember { mutableIntStateOf(10) }

    LaunchedEffect(isScanning)
    {
        if (isScanning)
        {
            secondsLeft = 10
            while (secondsLeft > 0)
            {
                delay(1000) // Wait 1 second safely in the background
                secondsLeft--
            }
            // When it hits 0, trigger the timeout callback back to MainActivity
            onScanStop(true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    )
    {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        )
        {
            // Live Status Text Tracker
            Text(
                text = if (isScanning) "$statusText (${secondsLeft}s remaining)" else statusText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp),
                color = Color.Red
            )

            if (discoveredAids.isNotEmpty())
            {
                Text(text = "Results:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth())
                {
                    items(discoveredAids)
                    { app ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp))
                        {
                            Column(modifier = Modifier.padding(16.dp))
                            {
                                Text(text = "AID: ${app.aid.joinToString("") { "%02X".format(it) }}")
                                Text(text = "Priority: ${app.priority}")

                                if (byteArray.isNotEmpty())
                                {
                                    Text(text = "Res: ${byteArray.joinToString("") { "%02X".format(it) }}")
                                }
                            }
                        }
                    }

                }
            }
        }

        // Trigger Button
        Button(
            onClick = { if (isScanning) onScanStop(false) else onScanStart() },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        if (isScanning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
            modifier = Modifier
                .fillMaxWidth()
        )
        {
            Text(if (isScanning) "Cancel Scan" else "Start Scan")
        }
    }
}