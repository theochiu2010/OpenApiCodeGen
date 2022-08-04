package com.ikigai.openApiCodeGenGrade.consumer.client

import org.openapitools.client.models.InlineResponse200
import org.openapitools.ikigai.api.PublicAPIsApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class NRSApiClient {
    @Value("\${contracts.nrs.baseUrl}")
    private lateinit var nrsBaseUrl: String

    lateinit var api: PublicAPIsApi

    fun getLocalityLang(): InlineResponse200 {
        api = PublicAPIsApi(nrsBaseUrl)

        return api.getLocalityConfig(BigDecimal(4))
    }
}