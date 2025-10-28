package com.grupob.futbolapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FutbolApiApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<FutbolApiApplication>(*args)
        }
    }
}