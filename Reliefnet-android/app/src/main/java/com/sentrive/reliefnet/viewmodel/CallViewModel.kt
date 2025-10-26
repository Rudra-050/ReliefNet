package com.sentrive.reliefnet.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.*

class CallViewModel(app: Application) : AndroidViewModel(app) {
    // Incoming call state
    private val _incomingCall = MutableStateFlow<IncomingCallInfo?>(null)
    val incomingCall: StateFlow<IncomingCallInfo?> = _incomingCall

    data class IncomingCallInfo(
        val fromUserId: String,
        val fromUserType: String,
        val callType: String
    )

    companion object {
        private const val TAG = "CallViewModel"
        // Use Railway production URL - will work on all devices/emulator
        private const val SERVER_URL = "https://reliefnet-production-e119.up.railway.app"
    }

    private var socket: Socket? = null
    // Track if this is a video or audio call
    var isVideoCall: Boolean = true

    // WebRTC
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoSource: VideoSource? = null
    private var localAudioSource: AudioSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
        private var videoCapturer: CameraVideoCapturer? = null
    var eglBase: EglBase? = null

    // State
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private val _localStream = MutableStateFlow<MediaStream?>(null)
    val localStream: StateFlow<MediaStream?> = _localStream

    private val _remoteStream = MutableStateFlow<MediaStream?>(null)
    val remoteStream: StateFlow<MediaStream?> = _remoteStream

    // Signaling
    var selfId: String = ""
    var peerId: String = ""
    var isCaller: Boolean = false

    var selfType: String = "patient" // or "doctor"
    var peerType: String = "doctor"
    
        // Audio state
        private val _isAudioMuted = MutableStateFlow(false)
        val isAudioMuted: StateFlow<Boolean> = _isAudioMuted

    init {
        initializeWebRTC()
        initializeSocket()
    }
    
        fun toggleMute() {
            _isAudioMuted.value = !_isAudioMuted.value
            localAudioTrack?.setEnabled(!_isAudioMuted.value)
            Log.d(TAG, "Audio ${if (_isAudioMuted.value) "muted" else "unmuted"}")
        }
    
            fun switchCamera() {
                videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
                    override fun onCameraSwitchDone(isFrontFacing: Boolean) {
                        Log.d(TAG, "Camera switched to ${if (isFrontFacing) "front" else "back"}")
                    }
                    override fun onCameraSwitchError(errorDescription: String?) {
                        Log.e(TAG, "Camera switch error: $errorDescription")
                    }
                })
            }

    private fun initializeSocket() {
        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
            }
            socket = IO.socket(SERVER_URL, options)
            setupSocketListeners()
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Socket initialization error", e)
        }
    }

    private fun setupSocketListeners() {
        socket?.apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Call socket connected")
                // Register user
                val data = JSONObject().apply {
                    put("userId", selfId)
                    put("userType", selfType)
                }
                emit("register", data)
            }

            on("call:incoming") { args ->
                val data = args[0] as JSONObject
                val fromUserId = data.getString("fromUserId")
                val fromUserType = data.getString("fromUserType")
                val callType = data.getString("callType")
                _incomingCall.value = IncomingCallInfo(fromUserId, fromUserType, callType)
                Log.d(TAG, "Incoming call: $fromUserId $callType")
            }

            on("call:offer") { args ->
                val data = args[0] as JSONObject
                val sdp = data.getJSONObject("offer").getString("sdp")
                val type = data.getJSONObject("offer").getString("type")
                val offer = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
                viewModelScope.launch { receiveOffer(offer) }
            }

            on("call:answer") { args ->
                val data = args[0] as JSONObject
                val sdp = data.getJSONObject("answer").getString("sdp")
                val type = data.getJSONObject("answer").getString("type")
                val answer = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
                viewModelScope.launch { receiveAnswer(answer) }
            }

            on("call:ice-candidate") { args ->
                val data = args[0] as JSONObject
                val candidate = data.getJSONObject("candidate")
                val ice = IceCandidate(
                    candidate.getString("sdpMid"),
                    candidate.getInt("sdpMLineIndex"),
                    candidate.getString("candidate")
                )
                viewModelScope.launch { addIceCandidate(ice) }
            }

            on("call:end") {
                Log.d(TAG, "Call ended by peer")
                viewModelScope.launch { endCall() }
            }
        }
    }

    fun acceptIncomingCall() {
        _incomingCall.value?.let { info ->
            peerId = info.fromUserId
            peerType = info.fromUserType
            isVideoCall = info.callType == "video"
            isCaller = false
            startLocalMedia()
            // Will wait for offer, then answer
            _callState.value = CallState.Connecting
        }
        _incomingCall.value = null
    }

    fun declineIncomingCall() {
        // Optionally notify peer
        _incomingCall.value = null
        _callState.value = CallState.Idle
    }

    private fun initializeWebRTC() {
        eglBase = EglBase.create()
        val options = PeerConnectionFactory.InitializationOptions.builder(getApplication())
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase!!.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun startLocalMedia() {
        val stream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")
        if (isVideoCall) {
            val videoCapturer = createCameraCapturer()
            localVideoSource = peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast == true)
            videoCapturer?.initialize(
                SurfaceTextureHelper.create("CaptureThread", eglBase!!.eglBaseContext),
                getApplication(),
                localVideoSource?.capturerObserver
            )
            videoCapturer?.startCapture(640, 480, 30)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", localVideoSource)
            stream?.addTrack(localVideoTrack)
        }
        localAudioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", localAudioSource)
        stream?.addTrack(localAudioTrack)
        _localStream.value = stream
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(getApplication())
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    videoCapturer = capturer as? CameraVideoCapturer
                    return capturer
            }
        }
        for (deviceName in enumerator.deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                    val capturer = enumerator.createCapturer(deviceName, null)
                    videoCapturer = capturer as? CameraVideoCapturer
                    return capturer
            }
        }
        return null
    }

    fun startCall(isVideo: Boolean) {
        isVideoCall = isVideo
        socket?.connect()
        
        // First, alert the recipient about incoming call
        val initiateData = JSONObject().apply {
            put("toUserId", peerId)
            put("toUserType", peerType)
            put("fromUserId", selfId)
            put("fromUserType", selfType)
            put("callType", if (isVideo) "video" else "audio")
        }
        socket?.emit("call:initiate", initiateData)
        Log.d(TAG, "Call initiated: $peerId ($peerType) - ${if (isVideo) "video" else "audio"}")
        
        // Create peer connection and offer
        createPeerConnection()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            // Send offer via socket
                            val offerJson = JSONObject().apply {
                                put("type", desc.type.canonicalForm())
                                put("sdp", desc.description)
                            }
                            val data = JSONObject().apply {
                                put("toUserId", peerId)
                                put("toUserType", peerType)
                                put("offer", offerJson)
                                put("fromUserId", selfId)
                                put("fromUserType", selfType)
                            }
                            socket?.emit("call:offer", data)
                        }
                        override fun onSetFailure(p0: String?) {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, desc)
                }
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
        _callState.value = CallState.Calling
    }

    fun receiveOffer(offer: SessionDescription) {
        createPeerConnection()
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                // Create answer
                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        if (desc != null) {
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onSetSuccess() {
                                    // Send answer via socket
                                    val answerJson = JSONObject().apply {
                                        put("type", desc.type.canonicalForm())
                                        put("sdp", desc.description)
                                    }
                                    val data = JSONObject().apply {
                                        put("toUserId", peerId)
                                        put("toUserType", peerType)
                                        put("answer", answerJson)
                                        put("fromUserId", selfId)
                                        put("fromUserType", selfType)
                                    }
                                    socket?.emit("call:answer", data)
                                }
                                override fun onSetFailure(p0: String?) {}
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onCreateFailure(p0: String?) {}
                            }, desc)
                        }
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
            }
            override fun onSetFailure(p0: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, offer)
        _callState.value = CallState.Connecting
    }

    fun receiveAnswer(answer: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                _callState.value = CallState.Connected
            }
            override fun onSetFailure(p0: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, answer)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun endCall() {
        // Close peer connection and notify peer
        peerConnection?.close()
        peerConnection = null
        val data = JSONObject().apply {
            put("toUserId", peerId)
            put("toUserType", peerType)
            put("fromUserId", selfId)
            put("fromUserType", selfType)
        }
        socket?.emit("call:end", data)
        _callState.value = CallState.Idle
    }

    private fun createPeerConnection() {
        if (peerConnection != null) return
        
        // Add STUN servers for NAT traversal
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    val candidateJson = JSONObject().apply {
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                        put("candidate", candidate.sdp)
                    }
                    val data = JSONObject().apply {
                        put("toUserId", peerId)
                        put("toUserType", peerType)
                        put("candidate", candidateJson)
                        put("fromUserId", selfId)
                        put("fromUserType", selfType)
                    }
                    socket?.emit("call:ice-candidate", data)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                _remoteStream.value = stream
            }
            override fun onRemoveStream(stream: MediaStream?) {
                _remoteStream.value = null
            }
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
        // Add local stream
        _localStream.value?.let { peerConnection?.addStream(it) }
    }

    sealed class CallState {
        object Idle : CallState()
        object Calling : CallState()
        object Connecting : CallState()
        object Connected : CallState()
        object Ended : CallState()
    }
}
