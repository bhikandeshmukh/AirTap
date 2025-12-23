package com.bhikan.airtap.server

import android.content.Context
import com.bhikan.airtap.data.model.*
import com.bhikan.airtap.data.repository.FileRepository
import com.bhikan.airtap.data.repository.SmsRepository
import com.bhikan.airtap.server.auth.AuthManager
import com.bhikan.airtap.server.websocket.WebSocketHandler
import com.bhikan.airtap.service.NotificationService
import com.bhikan.airtap.service.RemoteControlService
import com.bhikan.airtap.service.ScreenCaptureService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class WebServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileRepository: FileRepository,
    private val smsRepository: SmsRepository,
    private val authManager: AuthManager
) {
    private var server: ApplicationEngine? = null
    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

    val isRunning: Boolean get() = server != null

    fun start(port: Int = 8080) {
        if (server != null) return
        server = embeddedServer(Netty, port = port) { configureServer() }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    private fun Application.configureServer() {
        install(ContentNegotiation) { json(json) }
        install(WebSockets) { pingPeriod = Duration.ofSeconds(15); timeout = Duration.ofSeconds(15) }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(HttpStatusCode.InternalServerError, ApiError(cause.message ?: "Unknown error", 500))
            }
        }
        install(Authentication) {
            bearer("auth-bearer") {
                authenticate { tokenCredential ->
                    if (authManager.validateSession(tokenCredential.token)) UserIdPrincipal("user") else null
                }
            }
        }
        routing {
            publicRoutes()
            authenticatedRoutes()
        }
    }

    private fun Routing.authenticatedRoutes() {
        authenticate("auth-bearer") {
            route("/api/files") {
                get {
                    val path = call.parameters["path"] ?: ""
                    call.respond(fileRepository.listFiles(path))
                }
                get("/roots") { call.respond(fileRepository.getStorageRoots()) }
                get("/download") {
                    val path = call.parameters["path"]
                    if (path.isNullOrEmpty()) { call.respond(HttpStatusCode.BadRequest, ApiError("Path required", 400)); return@get }
                    val file = fileRepository.getFile(path)
                    if (file == null) { call.respond(HttpStatusCode.NotFound, ApiError("File not found", 404)); return@get }
                    call.response.header(HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString())
                    call.respondFile(file)
                }
                post("/upload") {
                    val path = call.parameters["path"] ?: ""
                    var uploadedFile: String? = null
                    call.receiveMultipart().forEachPart { part ->
                        if (part is PartData.FileItem) {
                            fileRepository.saveFile(path, part.originalFileName ?: "uploaded_file", part.streamProvider())
                                .onSuccess { uploadedFile = it }
                        }
                        part.dispose()
                    }
                    if (uploadedFile != null) call.respond(UploadResponse(true, "File uploaded", uploadedFile))
                }
                delete {
                    val path = call.parameters["path"]
                    if (path.isNullOrEmpty()) { call.respond(HttpStatusCode.BadRequest, ApiError("Path required", 400)); return@delete }
                    fileRepository.deleteFile(path)
                        .onSuccess { call.respond(DeleteResponse(true, "Deleted")) }
                        .onFailure { call.respond(HttpStatusCode.InternalServerError, DeleteResponse(false, it.message ?: "Failed")) }
                }
                post("/mkdir") {
                    val params = call.receiveParameters()
                    val name = params["name"]
                    if (name.isNullOrEmpty()) { call.respond(HttpStatusCode.BadRequest, ApiError("Name required", 400)); return@post }
                    fileRepository.createDirectory(params["path"] ?: "", name)
                        .onSuccess { call.respond(mapOf("success" to true, "path" to it)) }
                        .onFailure { call.respond(HttpStatusCode.InternalServerError, ApiError(it.message ?: "Failed", 500)) }
                }
            }
            
            route("/api/notifications") {
                get {
                    val notifications = NotificationService.getInstance()?.getNotificationsList() ?: emptyList()
                    call.respond(NotificationListResponse(notifications, notifications.size))
                }
                post("/dismiss") {
                    val params = call.receiveParameters()
                    val notificationId = params["id"]
                    if (notificationId.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("Notification ID required", 400))
                        return@post
                    }
                    NotificationService.dismissNotification(notificationId)
                    call.respond(mapOf("success" to true))
                }
                post("/dismiss-all") {
                    NotificationService.dismissAllNotifications()
                    call.respond(mapOf("success" to true))
                }
            }
            
            route("/api/sms") {
                get("/conversations") {
                    val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                    val conversations = smsRepository.getConversations(limit)
                    call.respond(SmsListResponse(conversations, conversations.size))
                }
                get("/thread/{threadId}") {
                    val threadId = call.parameters["threadId"]?.toLongOrNull()
                    if (threadId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("Invalid thread ID", 400))
                        return@get
                    }
                    val messages = smsRepository.getMessages(threadId)
                    val address = messages.firstOrNull()?.address ?: ""
                    val contactName = messages.firstOrNull()?.contactName
                    call.respond(SmsThreadResponse(threadId, address, contactName, messages))
                }
                get("/search") {
                    val query = call.parameters["q"] ?: ""
                    if (query.length < 2) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("Query too short", 400))
                        return@get
                    }
                    val messages = smsRepository.searchMessages(query)
                    call.respond(messages)
                }
                post("/send") {
                    val params = call.receiveParameters()
                    val address = params["address"]
                    val message = params["message"]
                    if (address.isNullOrEmpty() || message.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("Address and message required", 400))
                        return@post
                    }
                    smsRepository.sendSms(address, message)
                        .onSuccess { call.respond(SendSmsResponse(true, "SMS sent")) }
                        .onFailure { call.respond(HttpStatusCode.InternalServerError, SendSmsResponse(false, it.message ?: "Failed")) }
                }
                post("/mark-read/{threadId}") {
                    val threadId = call.parameters["threadId"]?.toLongOrNull()
                    if (threadId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("Invalid thread ID", 400))
                        return@post
                    }
                    smsRepository.markAsRead(threadId)
                    call.respond(mapOf("success" to true))
                }
            }
            
            route("/api/screen") {
                get("/status") {
                    call.respond(mapOf(
                        "streaming" to ScreenCaptureService.isStreaming.value,
                        "message" to if (ScreenCaptureService.isStreaming.value) "Screen mirroring active" else "Start mirroring from phone app"
                    ))
                }
                get("/frame") {
                    val frame = ScreenCaptureService.getLatestFrameBytes()
                    if (frame != null) {
                        call.respondBytes(frame, ContentType.Image.JPEG)
                    } else {
                        call.respond(HttpStatusCode.ServiceUnavailable, ApiError("Screen capture not active", 503))
                    }
                }
                get("/mjpeg") {
                    if (!ScreenCaptureService.isStreaming.value) {
                        call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Screen capture not active"))
                        return@get
                    }
                    call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                    call.response.header(HttpHeaders.Pragma, "no-cache")
                    call.response.header("Expires", "0")
                    call.respondBytesWriter(ContentType.parse("multipart/x-mixed-replace; boundary=--frame")) {
                        while (ScreenCaptureService.isStreaming.value) {
                            val frame = ScreenCaptureService.getLatestFrameBytes()
                            if (frame != null) {
                                try {
                                    writeStringUtf8("--frame\r\n")
                                    writeStringUtf8("Content-Type: image/jpeg\r\n")
                                    writeStringUtf8("Content-Length: ${frame.size}\r\n\r\n")
                                    writeFully(frame)
                                    writeStringUtf8("\r\n")
                                    flush()
                                } catch (e: Exception) {
                                    break
                                }
                            }
                            delay(100)
                        }
                    }
                }
            }
            
            route("/api/control") {
                get("/status") {
                    call.respond(mapOf(
                        "enabled" to RemoteControlService.isEnabled.value,
                        "message" to if (RemoteControlService.isEnabled.value) "Remote control active" else "Enable Accessibility Service in Settings"
                    ))
                }
                post("/tap") {
                    val params = call.receiveParameters()
                    val x = params["x"]?.toFloatOrNull()
                    val y = params["y"]?.toFloatOrNull()
                    if (x == null || y == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("x and y coordinates required", 400))
                        return@post
                    }
                    val success = RemoteControlService.performTap(x, y)
                    call.respond(mapOf("success" to success))
                }
                post("/swipe") {
                    val params = call.receiveParameters()
                    val startX = params["startX"]?.toFloatOrNull()
                    val startY = params["startY"]?.toFloatOrNull()
                    val endX = params["endX"]?.toFloatOrNull()
                    val endY = params["endY"]?.toFloatOrNull()
                    val duration = params["duration"]?.toLongOrNull() ?: 300
                    if (startX == null || startY == null || endX == null || endY == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("startX, startY, endX, endY required", 400))
                        return@post
                    }
                    val success = RemoteControlService.performSwipe(startX, startY, endX, endY, duration)
                    call.respond(mapOf("success" to success))
                }
                post("/longpress") {
                    val params = call.receiveParameters()
                    val x = params["x"]?.toFloatOrNull()
                    val y = params["y"]?.toFloatOrNull()
                    val duration = params["duration"]?.toLongOrNull() ?: 1000
                    if (x == null || y == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiError("x and y coordinates required", 400))
                        return@post
                    }
                    val success = RemoteControlService.performLongPress(x, y, duration)
                    call.respond(mapOf("success" to success))
                }
                post("/back") {
                    val success = RemoteControlService.pressBack()
                    call.respond(mapOf("success" to success))
                }
                post("/home") {
                    val success = RemoteControlService.pressHome()
                    call.respond(mapOf("success" to success))
                }
                post("/recents") {
                    val success = RemoteControlService.pressRecents()
                    call.respond(mapOf("success" to success))
                }
                post("/notifications") {
                    val success = RemoteControlService.openNotifications()
                    call.respond(mapOf("success" to success))
                }
                post("/quicksettings") {
                    val success = RemoteControlService.openQuickSettings()
                    call.respond(mapOf("success" to success))
                }
                post("/lock") {
                    val success = RemoteControlService.lockScreen()
                    call.respond(mapOf("success" to success))
                }
                post("/screenshot") {
                    val success = RemoteControlService.takeScreenshot()
                    call.respond(mapOf("success" to success))
                }
            }
            
            webSocket("/ws") {
                val sessionId = UUID.randomUUID().toString()
                WebSocketHandler.addSession(sessionId, this)
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            // Handle incoming messages if needed
                        }
                    }
                } finally {
                    WebSocketHandler.removeSession(sessionId)
                }
            }
        }
    }

    private fun Routing.publicRoutes() {
        get("/api/health") { call.respond(mapOf("status" to "ok", "version" to "1.0.0")) }
        
        // Email-only login (for desktop app) - simple pairing
        post("/api/login") {
            val params = call.receiveParameters()
            val email = params["email"]?.lowercase()?.trim() ?: ""
            val deviceId = params["deviceId"] ?: java.util.UUID.randomUUID().toString()
            
            // Check if superadmin OR email matches registered email
            if (authManager.validateEmail(email)) {
                // Email matches or is superadmin - create session
                val clientIp = call.request.local.localHost
                val token = authManager.createSession(clientIp, deviceId)
                val isSuperAdmin = authManager.isSuperAdmin(email)
                call.respond(mapOf(
                    "token" to token,
                    "success" to true,
                    "isSuperAdmin" to isSuperAdmin,
                    "message" to if (isSuperAdmin) "Superadmin access granted" else "Connected successfully"
                ))
            } else {
                call.respond(HttpStatusCode.Unauthorized, ApiError(
                    "Email not registered. Make sure you use the same email as on your phone.", 401
                ))
            }
        }
        
        // Get device info for connection
        get("/api/device/info") {
            val email = authManager.userEmail
            if (email != null) {
                call.respond(mapOf(
                    "registered" to true,
                    "email" to email
                ))
            } else {
                call.respond(mapOf("registered" to false))
            }
        }
        
        post("/api/logout") {
            call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")?.let { authManager.invalidateSession(it) }
            call.respond(mapOf("success" to true))
        }
        get("/") { call.respondText(getFullIndexHtml(), ContentType.Text.Html) }
    }
}
