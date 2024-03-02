package shortlinkapp.api.service.shortlink.actions

import ca.jois.shortlink.manager.ShortLinkManager
import ca.jois.shortlink.model.ShortLink
import ca.jois.shortlink.model.ShortLinkUser
import ca.jois.shortlink.persistence.ShortLinkStore

class ListShortLinksAction(private val shortLinkManager: ShortLinkManager) {
    fun handle(request: Request): Response {
        return Response.of(
            shortLinkManager.listByOwner(request.ownerAsShortLinkUser(), request.paginationKey)
        )
    }

    data class Request(val owner: String? = null, val paginationKey: String? = null) {
        fun ownerAsShortLinkUser() = owner?.let { ShortLinkUser(it) } ?: ShortLinkUser.ANONYMOUS
    }

    data class Response(val entries: List<ShortLink>, val paginationKey: String?) {
        companion object {
            fun of(paginatedResult: ShortLinkStore.PaginatedResult<ShortLink>) =
                Response(
                    entries = paginatedResult.entries,
                    paginationKey = paginatedResult.paginationKey
                )
        }
    }
}
