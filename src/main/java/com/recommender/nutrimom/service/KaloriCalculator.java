package com.recommender.nutrimom.service;

import com.recommender.nutrimom.model.FoodInput;
import org.springframework.stereotype.Component;

@Component
public class KaloriCalculator {


    public double calculateAMB(FoodInput input) {
        return 655 + (9.6 * input.getBeratBadan()) + (1.8 * input.getTinggiBadan()) - (4.7 * input.getUsia());
    }

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

        double tambahanKalori;
        int usiaKehamilan = input.getUsiaKehamilan();

        if (usiaKehamilan >= 0 && usiaKehamilan <= 13) {
            tambahanKalori = 180;
        } else if (usiaKehamilan >= 14 && usiaKehamilan <= 27) {
            tambahanKalori = 300;
        } else if (usiaKehamilan >= 28) {
            tambahanKalori = 300;
        } else {
            tambahanKalori = 0; // fallback jika data tidak valid
        }

        return (amb * faktorAktivitas * faktorStres) + tambahanKalori;
    }

    public Nutrisi calculateNutrisi(double tee, int usiaKehamilan) {
        // Tambahan dari tabel 2.3
        double tambahanKarbo = 0;
        double tambahanLemak = 0;
        double tambahanProtein = 0;

        if (usiaKehamilan >= 0 && usiaKehamilan <= 13) { // Trimester 1
            tambahanKarbo = 25;
            tambahanLemak = 2;
            tambahanProtein = 1;
        } else if (usiaKehamilan >= 14 && usiaKehamilan <= 27) { // Trimester 2
            tambahanKarbo = 40;
            tambahanLemak = 2;
            tambahanProtein = 10;
        } else if (usiaKehamilan >= 28) { // Trimester 3
            tambahanKarbo = 40;
            tambahanLemak = 2;
            tambahanProtein = 30;
        }

        double kebutuhanKarbo = ((0.60 * tee) / 4.0) + tambahanKarbo;
        double kebutuhanLemak = ((0.25 * tee) / 9.0) + tambahanLemak;
        double kebutuhanProtein = ((0.15 * tee) / 4.0) + tambahanProtein;

        return new Nutrisi(kebutuhanKarbo, kebutuhanLemak, kebutuhanProtein);
    }

    public static class Nutrisi {
        private final double karbohidrat;
        private final double lemak;
        private final double protein;

        public Nutrisi(double karbohidrat, double lemak, double protein) {
            this.karbohidrat = karbohidrat;
            this.lemak = lemak;
            this.protein = protein;
        }

        public double getKarbohidrat() {
            return karbohidrat;
        }

        public double getLemak() {
            return lemak;
        }

        public double getProtein() {
            return protein;
        }
    }
}
