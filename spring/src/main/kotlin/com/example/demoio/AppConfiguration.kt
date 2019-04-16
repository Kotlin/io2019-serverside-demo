package com.example.demoio

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import javax.servlet.Filter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Configuration
@PropertySource("classpath:secret.properties")
class AppConfiguration(
        @Value("\${secret.word}") private val secretWord : String
) {
    @Bean
    fun secretFilter() : Filter = Filter { req, resp, chain ->
        val httpReq = req as HttpServletRequest
        val httpResp = resp as HttpServletResponse

        if ("GET" != req.method) {
            val secret = httpReq.getHeader("x-authorization")
            if (secret != secretWord) {
                httpResp.sendError(403)
                return@Filter
            }
        }
        chain.doFilter(req, resp)
    }
}