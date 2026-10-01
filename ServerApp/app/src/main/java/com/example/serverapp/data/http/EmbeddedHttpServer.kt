package com.example.serverapp.data.http

import com.example.serverapp.core.Logger
import fi.iki.elonen.NanoHTTPD

/**
 * The single point of contact with NanoHTTPD.
 *
 * NanoHTTPD runs its own listener thread and hands each request to [serve] on a
 * worker thread, so nothing here touches the main thread. The work is delegated
 * straight to [RouteHandler], keeping the library-specific code to the body
 * reading and the response construction.
 */
class EmbeddedHttpServer(
    port: Int,
    private val routeHandler: RouteHandler,
    private val logger: Logger,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val body = readBody(session)

        val reply = try {
            routeHandler.handle(
                HttpRequest(
                    method = session.method.name,
                    path = session.uri,
                    body = body,
                ),
            )
        } catch (t: Throwable) {
            // A handler crash must not kill the server thread; report 500 instead.
            logger.e("SERVER handler failed", t)
            HttpReply(
                statusCode = 500,
                body = """{"error":"Internal server error"}""",
            )
        }

        return newFixedLengthResponse(
            reply.statusCode.toNanoStatus(),
            reply.contentType,
            reply.body,
        )
    }

    /**
     * Reads the request body.
     *
     * NanoHTTPD requires [IHTTPSession.parseBody] to be called before the body
     * is available; for JSON payloads it places the raw text under the
     * "postData" key rather than treating it as a form field.
     */
    private fun readBody(session: IHTTPSession): String {
        if (session.method != Method.POST && session.method != Method.PUT) {
            return ""
        }
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            files["postData"].orEmpty()
        } catch (t: Throwable) {
            logger.w("SERVER could not read request body: ${t.message}")
            ""
        }
    }

    /** Maps our integer status onto NanoHTTPD's status enum. */
    private fun Int.toNanoStatus(): Response.IStatus =
        Response.Status.entries.firstOrNull { it.requestStatus == this }
            ?: Response.Status.INTERNAL_ERROR
}
