package com.ikigai.openApiCodeGenGrade.consumer

import org.openapitools.ikigai.api.PlanetsApi
import org.openapitools.model.Planet
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
class PlanetController : PlanetsApi {
    override fun allPlanets(): ResponseEntity<List<Planet>> {
        var result = listOf(
            Planet(
                name = "flower",
                position = BigDecimal(1),
                moons = BigDecimal(20.2)
            )
        )

        return ResponseEntity(result, HttpStatus.OK)
    }

    override fun onePlanet(planetId: BigDecimal): ResponseEntity<Planet> {
        return super.onePlanet(planetId)
    }
}