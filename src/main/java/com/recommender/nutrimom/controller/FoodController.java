package com.recommender.nutrimom.controller;

import com.recommender.nutrimom.model.FoodInput;
import com.recommender.nutrimom.service.FoodService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping("/recommendation")
    public Map<String, Object> getRecommendation(@RequestBody FoodInput input) throws Exception {
        return foodService.getRecommendations(input);
    }
}

