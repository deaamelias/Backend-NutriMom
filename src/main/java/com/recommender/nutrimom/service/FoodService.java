package com.recommender.nutrimom.service;

import com.recommender.nutrimom.model.FoodInput;
import com.recommender.nutrimom.util.OWLUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodService {

    private static final double SARAPAN_PERCENTAGE = 0.25;
    private static final double MAKAN_SIANG_PERCENTAGE = 0.30;
    private static final double MAKAN_MALAM_PERCENTAGE = 0.25;
    private static final double CEMILAN_PERCENTAGE = 0.20;
    private static final double MAX_PORTION = 200;
    private static final double MIN_PORTION = 50;
    private static final double MAX_CALORIE_DIFFERENCE = 20;

    private final KaloriCalculator kaloriCalculator;
    private final Set<String> terpilihSebelumnya = new HashSet<>();

    public FoodService(KaloriCalculator kaloriCalculator) {
        this.kaloriCalculator = kaloriCalculator;
    }

    public Map<String, Object> getRecommendations(FoodInput input) {
        Map<String, Object> result = new HashMap<>();

        // 🔥 Hitung AMB dan TEE menggunakan KaloriCalculator
        double amb = kaloriCalculator.calculateAMB(input);
        double tee = kaloriCalculator.calculateTEE(input, amb);

        // Hitung distribusi kalori berdasarkan persentase
        double kaloriSarapan = tee * SARAPAN_PERCENTAGE;
        double kaloriMakanSiang = tee * MAKAN_SIANG_PERCENTAGE;
        double kaloriMakanMalam = tee * MAKAN_MALAM_PERCENTAGE;
        double kaloriCemilan = tee * CEMILAN_PERCENTAGE;

        result.put("amb", amb);
        result.put("tee", tee);
        result.put("kaloriSarapan", kaloriSarapan);
        result.put("kaloriMakanSiang", kaloriMakanSiang);
        result.put("kaloriMakanMalam", kaloriMakanMalam);
        result.put("kaloriCemilan", kaloriCemilan);

        // Ambil rekomendasi makanan dari OWL
        boolean diabetesGestasional = "ya".equalsIgnoreCase(input.getDiabetesGestasional());
        Set<String> alergi = new HashSet<>(input.getAlergi());

        result.put("Sarapan", selectMeal("Sarapan", kaloriSarapan, alergi, diabetesGestasional));
        result.put("MakanSiang", selectMeal("Makan Siang", kaloriMakanSiang, alergi, diabetesGestasional));
        result.put("MakanMalam", selectMeal("Makan Malam", kaloriMakanMalam, alergi, diabetesGestasional));
        result.put("Cemilan", selectItems("Cemilan", kaloriCemilan, alergi, diabetesGestasional, 2));

        return result;
    }

    private Map<String, List<Map<String, String>>> selectMeal(String mealType, double targetKalori, Set<String> alergi, boolean diabetesGestasional) {
        Map<String, List<Map<String, String>>> meal = new HashMap<>();

        // Wajib ada minimal 1 item per kategori
        meal.put("MakananPokok", selectItems("MakananPokok", targetKalori / 4, alergi, diabetesGestasional, 1));
        meal.put("Lauk", selectItems("Lauk", targetKalori / 3, alergi, diabetesGestasional, 2));
        meal.put("Sayur", selectItems("Sayur", targetKalori / 4, alergi, diabetesGestasional, 1));
        meal.put("Buah", selectItems("Buah", targetKalori / 4, alergi, diabetesGestasional, 1));

        return meal;
    }



    private List<Map<String, String>> selectItems(String category, double targetKalori, Set<String> alergi, boolean diabetesGestasional, int maxItems) {
        List<Map<String, String>> selectedItems = new ArrayList<>();
        Set<Map<String, String>> foodSet = OWLUtil.getRecommendedFoods(category, alergi, diabetesGestasional);

        if (foodSet.isEmpty()) {
            // Ambil item pertama sebagai fallback jika kosong
            Map<String, String> fallback = new HashMap<>();
            fallback.put("nama", "Tidak ada rekomendasi tersedia");
            fallback.put("kalori", "0");
            fallback.put("protein", "0");
            fallback.put("karbo", "0");
            fallback.put("lemak", "0");
            selectedItems.add(fallback);
            return selectedItems;
        }

        List<Map<String, String>> sortedFoods = new ArrayList<>(foodSet);
        sortedFoods.sort(Comparator.comparingDouble(f -> Math.abs(targetKalori - Double.parseDouble(f.get("kalori")))));

        double totalKalori = 0;
        for (Map<String, String> food : sortedFoods) {
            if (selectedItems.size() >= maxItems) break;

            double kalori = Double.parseDouble(food.get("kalori"));
            String nama = food.get("nama");

            if (!terpilihSebelumnya.contains(nama)) {
                double porsi = Math.min(MAX_PORTION, Math.max(MIN_PORTION, (targetKalori / kalori) * 100));
                double kaloriSetelahPenyesuaian = totalKalori + (kalori * (porsi / 100));

                if (Math.abs(kaloriSetelahPenyesuaian - targetKalori) <= MAX_CALORIE_DIFFERENCE) {
                    Map<String, String> item = new HashMap<>();
                    item.put("nama", nama);
                    item.put("kalori", String.format("%.0f", kalori * (porsi / 100)));
                    item.put("porsi", String.format("%.1f gram", porsi));
                    item.put("protein", String.format("%.1f", Double.parseDouble(food.get("protein")) * (porsi / 100)));
                    item.put("karbo", String.format("%.1f", Double.parseDouble(food.get("karbo")) * (porsi / 100)));
                    item.put("lemak", String.format("%.1f", Double.parseDouble(food.get("lemak")) * (porsi / 100)));

                    selectedItems.add(item);
                    totalKalori += kalori * (porsi / 100);
                    terpilihSebelumnya.add(nama);
                }
            }
        }

        if (selectedItems.isEmpty() && !sortedFoods.isEmpty()) {
            Map<String, String> fallback = sortedFoods.get(0);
            selectedItems.add(fallback);
            terpilihSebelumnya.add(fallback.get("nama"));
        }

        return selectedItems;
    }



}
