package com.norineh.smsgateway

object Config {

    const val SERVER_BASE_URL = "https://norineh.com"

    const val PENDING_URL =
        "$SERVER_BASE_URL/api/sms/pending.php"

    const val RESULT_URL =
        "$SERVER_BASE_URL/api/sms/result.php"

    const val GATEWAY_TOKEN =
        "NORINEH-GW-2026-8f3c1a7e6d2b4c9e5a1f0b7d3c6e8a2"

    const val POLL_INTERVAL_MS = 5000L

    const val HTTP_TIMEOUT_SECONDS = 20L
}
