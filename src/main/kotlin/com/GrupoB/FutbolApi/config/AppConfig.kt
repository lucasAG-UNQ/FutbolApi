package com.grupob.futbolapi.config

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {

    @Bean
    fun okHttpClient(): OkHttpClient {
        return OkHttpClient()
    }
}