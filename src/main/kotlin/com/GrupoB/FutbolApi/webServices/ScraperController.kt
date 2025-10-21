package com.grupob.futbolapi.webServices

import com.grupob.futbolapi.services.ScraperService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/scraper")
class ScraperController(private val scraperService: WhoScoredScraperService) {
    private val logger = LoggerFactory.getLogger(ScraperController::class.java)
    @PostMapping("/scrape")
    fun scrapeData(): ResponseEntity<String> {
        logger.info("POST /api/scrape received")
        //scraperService.scrapeAndSaveData()
        return ResponseEntity.ok("Data scraped and saved successfully")
    }

}