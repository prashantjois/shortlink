package model

import java.net.URL

data class ShortLink(
    val originalUrl: URL,
    val shortCode: ShortCode,
    val createdAt: Long,
    val expiresAt: Long?,
)
