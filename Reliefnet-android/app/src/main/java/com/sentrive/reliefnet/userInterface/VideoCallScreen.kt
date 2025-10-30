package com.sentrive.reliefnet.userInterface

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentrive.reliefnet.viewmodel.CallViewModel
import org.webrtc.SurfaceViewRenderer
import com.sentrive.reliefnet.utils.TokenManager

@Composable
fun VideoCallScreen(
    selfId: String,
    peerId: String,
    isCaller: Boolean,
    callType: String = "video", // or "audio"
    viewModel: CallViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callState by viewModel.callState.collectAsState()
    val localStream by viewModel.localStream.collectAsState()
    val remoteStream by viewModel.remoteStream.collectAsState()
    val incomingCall by viewModel.incomingCall.collectAsState()
        val isAudioMuted by viewModel.isAudioMuted.collectAsState()

    // Permissions
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Start local media on first load
    LaunchedEffect(Unit) {
        // Determine self/peer types from logged-in user
        val userType = TokenManager.getUserType(context)
        if (userType == "doctor") {
            viewModel.selfType = "doctor"
            viewModel.peerType = "patient"
        } else {
            viewModel.selfType = "patient"
            viewModel.peerType = "doctor"
        }

        viewModel.selfId = selfId
        viewModel.peerId = peerId
        viewModel.isCaller = isCaller
        if (isCaller) {
            viewModel.startLocalMedia()
            viewModel.startCall(callType == "video")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Incoming call overlay
        if (incomingCall != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Incoming " + incomingCall!!.callType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Call", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = { viewModel.acceptIncomingCall() }) {
                            Icon(painterResource(android.R.drawable.ic_menu_call), contentDescription = "Accept", tint = Color.Green)
                            Spacer(Modifier.width(8.dp))
                            Text("Accept")
                        }
                        Spacer(Modifier.width(24.dp))
                        Button(onClick = { viewModel.declineIncomingCall() }) {
                            Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Decline", tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text("Decline")
                        }
                    }
                }
            }
            return@Box
        }
        // Remote video
        if (callType == "video") {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(viewModel.eglBase?.eglBaseContext, null)
                        remoteStream?.videoTracks?.firstOrNull()?.addSink(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        // Local video (small preview)
        if (callType == "video") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(120.dp, 160.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(viewModel.eglBase?.eglBaseContext, null)
                            localStream?.videoTracks?.firstOrNull()?.addSink(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // Call controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
                IconButton(onClick = { viewModel.toggleMute() }) {
                Icon(
                        imageVector = if (isAudioMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isAudioMuted) "Unmute" else "Mute",
                        tint = if (isAudioMuted) Color.Red else Color.White
                )
            }
            Spacer(Modifier.width(32.dp))
            IconButton(onClick = { viewModel.endCall() }) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.Red
                )
            }
            Spacer(Modifier.width(32.dp))
            if (callType == "video") {
                    IconButton(onClick = { viewModel.switchCamera() }) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }
        // Call state overlay
        if (callState != CallViewModel.CallState.Connected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (callState) {
                        CallViewModel.CallState.Calling -> "Calling..."
                        CallViewModel.CallState.Connecting -> "Connecting..."
                        CallViewModel.CallState.Ended -> "Call Ended"
                        else -> ""
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }
    }
}
