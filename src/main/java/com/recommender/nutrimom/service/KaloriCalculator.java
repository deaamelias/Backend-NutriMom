package com.recommender.nutrimom.service;

import com.recommender.nutrimom.model.FoodInput;
import org.springframework.stereotype.Component;

@Component
public class KaloriCalculator {

    /**
     * Hitung AMB berdasarkan rumus Harris-Benedict.
     */
    public double calculateAMB(FoodInput input) {
        return 655 + (9.6 * input.getBeratBadan()) + (1.85 * input.getTinggiBadan()) - (4.7 * input.getUsia());
    }

    /**
     * Hitung TEE berdasarkan aktivitas, stres, dan usia kehamilan.
     */
    public double calculateTEE(FoodInput input, double amb) {
        double faktorAktivitas = switch (input.getLevelAktivitas().toLowerCase()) {
            case "ditempattidur" -> 1.2;
            case "sangatringan" -> 1.3;
            case "ringan" -> 1.55;
            case "sedang" -> 1.7;
            case "berat" -> 2.0;
            default -> 1.2;
        };

        double faktorStres = switch (input.getKategoriStres().toLowerCase()) {
            case "tidakstres" -> 1.3;
            case "stresringan" -> 1.4;
            case "stressedang" -> 1.5;
            case "stresberat" -> 1.6;
            case "stressangatberat" -> 1.7;
            case "lukabakarserius" -> 2.1;
            default -> 1.3;
        };

        double tambahanKalori = switch (input.getUsiaKehamilan()) {
            case 1, 13 -> 180;
            case 14, 27 -> 300;
            default -> 300;
        };

        return (amb * faktorAktivitas * faktorStres) + tambahanKalori;
    }


}

