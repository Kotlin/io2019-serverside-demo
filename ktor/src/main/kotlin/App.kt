package com.jetbrains.ktorServer



fun main(args: Array<String>): Unit = io.ktor.server.jetty.EngineMain.main(arrayOf("-config=debug.conf"))
