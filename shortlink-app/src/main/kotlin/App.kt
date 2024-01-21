package shortlinkapp

import shortlinkapp.api.service.WebServer

fun main() {
    WebServer(8080).run()
}
