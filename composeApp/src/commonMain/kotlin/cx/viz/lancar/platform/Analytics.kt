package cx.viz.lancar.platform

/**
 * Screen-view analytics → self-hosted Umami (https://analytics.nextgensoft.co,
 * website "Lancar"). One fire-and-forget POST per screen: no cookies, no
 * identifiers, nothing beyond the screen name plus app/OS version in the
 * User-Agent. Failures are swallowed. Off by default so tests and previews never
 * report; the platform entry points (MainActivity / MainViewController) turn it on.
 */
object Analytics {
    var enabled = false

    fun screen(name: String) {
        if (!enabled) return
        val body = """{"type":"event","payload":{"website":"$WEBSITE_ID","hostname":"$HOSTNAME","url":"/$name","title":"$name"}}"""
        postJson("https://analytics.nextgensoft.co/api/send", body, userAgent)
    }

    private const val WEBSITE_ID = "8e8717ea-51be-408a-8d8a-3071585bb264"
    private const val HOSTNAME = "cx.viz.lancar"
}

/** Browser-shaped so Umami's bot filter keeps it; carries OS + app version. */
internal expect val userAgent: String

/** Fire-and-forget JSON POST: never throws, never blocks the caller. */
internal expect fun postJson(url: String, body: String, userAgent: String)
