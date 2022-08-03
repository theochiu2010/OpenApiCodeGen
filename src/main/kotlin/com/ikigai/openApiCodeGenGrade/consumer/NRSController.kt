package com.ikigai.openApiCodeGenGrade.consumer

import com.ikigai.openApiCodeGenGrade.consumer.client.NRSApiClient
import org.openapitools.client.models.InlineResponse200
import org.openapitools.ikigai.api.PublicAPIsApi
import org.openapitools.model.Planet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("\${api.base-path:}")
class NRSController {
    @Autowired
    lateinit var client: NRSApiClient

    @GetMapping(
        value = ["/nrs/localityConfig"],
        produces = ["application/json"]
    )
    fun getLocalityConfig(): ResponseEntity<InlineResponse200> {
        return ResponseEntity(client.getLocalityLang(), HttpStatus.OK)
    }
}