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

    private final KaloriCalculator kaloriCalculator;
    private final Set<String> terpilihSebelumnya = new HashSet<>();

    public FoodService(KaloriCalculator kaloriCalculator) {
        this.kaloriCalculator = kaloriCalculator;
    }

    public Map<String, Object> getRecommendations(FoodInput input) {
        Map<String, Object> result = new HashMap<>();

        double amb = kaloriCalculator.calculateAMB(input);
        double tee = kaloriCalculator.calculateTEE(input, amb);

        KaloriCalculator.Nutrisi nutrisi = kaloriCalculator.calculateNutrisi(tee, input.getUsiaKehamilan());
        double karbohidrat = nutrisi.getKarbohidrat();
        double lemak = nutrisi.getLemak();
        double protein = nutrisi.getProtein();

        double kaloriSarapan = tee * SARAPAN_PERCENTAGE;
        double kaloriMakanSiang = tee * MAKAN_SIANG_PERCENTAGE;
        double kaloriMakanMalam = tee * MAKAN_MALAM_PERCENTAGE;
        double kaloriCemilan = tee * CEMILAN_PERCENTAGE;

        result.put("amb", amb);
        result.put("tee", tee);
        result.put("kebutuhanKarbohidrat", karbohidrat);
        result.put("kebutuhanLemak", lemak);
        result.put("kebutuhanProtein", protein);
        result.put("kaloriSarapan", kaloriSarapan);
        result.put("kaloriMakanSiang", kaloriMakanSiang);
        result.put("kaloriMakanMalam", kaloriMakanMalam);
        result.put("kaloriCemilan", kaloriCemilan);


        Set<String> alergi = new HashSet<>();
        if (input.getAlergi() != null) {
            for (String item : input.getAlergi()) {
                String trimmed = item.trim().toLowerCase();
                if (!trimmed.equals("tidak") && !trimmed.equals("tidak ada") && !trimmed.equals("none")) {
                    alergi.add(item);
                }
            }
        }

        result.put("Sarapan", selectMeal("Sarapan", kaloriSarapan, alergi));
        result.put("MakanSiang", selectMeal("Makan Siang", kaloriMakanSiang, alergi));
        result.put("MakanMalam", selectMeal("Makan Malam", kaloriMakanMalam, alergi));
        result.put("Cemilan", selectItems("Cemilan", kaloriCemilan, karbohidrat * CEMILAN_PERCENTAGE,
                protein * CEMILAN_PERCENTAGE, lemak * CEMILAN_PERCENTAGE, alergi, 1, 2));

        // Tambahkan evaluasi nutrisi di sini
        Map<String, Object> evaluasi = evaluateTotalNutrisi(result);
        result.put("evaluasiNutrisi", evaluasi);

        return result;
    }

    private Map<String, List<Map<String, String>>> selectMeal(String mealType, double targetKalori, Set<String> alergi) {
        Map<String, List<Map<String, String>>> meal = new HashMap<>();

        double targetKarbo = 0.60 * targetKalori / 4;
        double targetProtein = 0.15 * targetKalori / 4;
        double targetLemak = 0.25 * targetKalori / 9;

        // Adjust meal composition, especially for lauk (main dishes)
        meal.put("MakananPokok", selectItems("MakananPokok", targetKalori / 4, targetKarbo / 4, targetProtein / 4, targetLemak / 4, alergi, 1, 1));
        meal.put("Lauk", selectItems("Lauk", targetKalori / 3, targetKarbo / 3, targetProtein / 3, targetLemak / 3, alergi, 1, 2)); // Max 2 lauk
        meal.put("Sayur", selectItems("Sayur", targetKalori / 4, targetKarbo / 4, targetProtein / 4, targetLemak / 4, alergi, 1, 1));
        meal.put("Buah", selectItems("Buah", targetKalori / 4, targetKarbo / 4, targetProtein / 4, targetLemak / 4, alergi, 1, 1));

        return meal;
    }

    private List<Map<String, String>> selectItems(
            String category,
            double targetKalori,
            double targetKarbo,
            double targetProtein,
            double targetLemak,
            Set<String> alergi,
            int minItems,
            int maxItems) {

        List<Map<String, String>> selectedItems = new ArrayList<>();
        Set<Map<String, String>> foodSet = OWLUtil.getRecommendedFoods(category, alergi);

        if (foodSet.isEmpty()) {
            Map<String, String> fallback = new HashMap<>();
            fallback.put("nama", "Tidak ada rekomendasi tersedia");
            fallback.put("kalori", "0");
            fallback.put("protein", "0");
            fallback.put("karbo", "0");
            fallback.put("lemak", "0");
            fallback.put("porsi", "0 gram");
            selectedItems.add(fallback);
            return selectedItems;
        }

        List<Map<String, String>> foodList = new ArrayList<>(foodSet);
        List<Map<String, String>> bestCombo = new ArrayList<>();
        double bestScore = Double.MAX_VALUE;

        double[] porsiScales = {1.0, 0.75, 0.5};

        for (int n = minItems; n <= maxItems; n++) {
            List<List<Map<String, String>>> combinations = generateCombinations(foodList, n);

            for (List<Map<String, String>> combo : combinations) {
                boolean containsDuplicate = combo.stream()
                        .map(f -> f.get("nama"))
                        .anyMatch(terpilihSebelumnya::contains);
                if (containsDuplicate) continue;

                List<List<Double>> porsiCombinations = generatePorsiCombinations(porsiScales, combo.size());

                for (List<Double> porsis : porsiCombinations) {
                    double totalKal = 0, totalKarbo = 0, totalProtein = 0, totalLemak = 0;

                    for (int i = 0; i < combo.size(); i++) {
                        Map<String, String> item = combo.get(i);
                        double scale = porsis.get(i);

                        totalKal += parseDoubleSafe(item.get("kalori")) * scale;
                        totalKarbo += parseDoubleSafe(item.get("karbo")) * scale;
                        totalProtein += parseDoubleSafe(item.get("protein")) * scale;
                        totalLemak += parseDoubleSafe(item.get("lemak")) * scale;
                    }

                    // Batas bawah dan atas (95% - 110%)
                    boolean isUnderMin = totalKal < targetKalori * 0.95 ||
                            totalKarbo < targetKarbo * 0.95 ||
                            totalProtein < targetProtein * 0.95 ||
                            totalLemak < targetLemak * 0.95;

                    boolean isOverMax = totalKal > targetKalori * 1.1 ||
                            totalKarbo > targetKarbo * 1.1 ||
                            totalProtein > targetProtein * 1.1 ||
                            totalLemak > targetLemak * 1.1;

                    if (isOverMax) continue;

                    // Penalti jika di bawah 95%
                    double skorKal = Math.abs(totalKal - targetKalori) / (targetKalori + 1);
                    double skorKarbo = Math.abs(totalKarbo - targetKarbo) / (targetKarbo + 1);
                    double skorProtein = Math.abs(totalProtein - targetProtein) / (targetProtein + 1);
                    double skorLemak = Math.abs(totalLemak - targetLemak) / (targetLemak + 1);

                    if (isUnderMin) {
                        skorKal *= 2;
                        skorKarbo *= 2;
                        skorProtein *= 2;
                        skorLemak *= 2;
                    }

                    double totalScore = (skorKal * 0.2) + (skorKarbo * 0.3) + (skorProtein * 0.3) + (skorLemak * 0.2);

                    if (totalScore < bestScore) {
                        bestScore = totalScore;
                        bestCombo.clear();
                        for (int i = 0; i < combo.size(); i++) {
                            Map<String, String> item = new HashMap<>(combo.get(i));
                            double scale = porsis.get(i);
                            item.put("porsi", String.format("%.0f", 100 * scale));
                            bestCombo.add(item);
                        }
                    }
                }
            }
        }

        // Tambahkan makanan tambahan jika belum memenuhi 95% target
        selectedItems.addAll(bestCombo);
        double sumKal = 0, sumKarbo = 0, sumProtein = 0, sumLemak = 0;
        for (Map<String, String> item : selectedItems) {
            double porsi = parseDoubleSafe(item.get("porsi")) / 100.0;
            sumKal += parseDoubleSafe(item.get("kalori")) * porsi;
            sumKarbo += parseDoubleSafe(item.get("karbo")) * porsi;
            sumProtein += parseDoubleSafe(item.get("protein")) * porsi;
            sumLemak += parseDoubleSafe(item.get("lemak")) * porsi;
        }

        if (sumProtein < targetProtein * 0.95 || sumKarbo < targetKarbo * 0.95 || sumLemak < targetLemak * 0.95) {
            for (Map<String, String> candidate : foodList) {
                if (terpilihSebelumnya.contains(candidate.get("nama"))) continue;

                double protein = parseDoubleSafe(candidate.get("protein"));
                double kalori = parseDoubleSafe(candidate.get("kalori"));

                if (protein >= 8.0 && kalori <= 150) {
                    Map<String, String> tambahan = new HashMap<>(candidate);
                    tambahan.put("porsi", "100"); // asumsikan full porsi
                    selectedItems.add(tambahan);
                    terpilihSebelumnya.add(tambahan.get("nama"));
                    break;
                }
            }
        }

        if (selectedItems.isEmpty() && !foodList.isEmpty()) {
            selectedItems.add(foodList.get(0));
        }

        return selectedItems;
    }

    private List<List<Double>> generatePorsiCombinations(double[] scales, int size) {
        List<List<Double>> result = new ArrayList<>();
        backtrackPorsi(scales, size, new ArrayList<>(), result);
        return result;
    }

    private void backtrackPorsi(double[] scales, int size, List<Double> temp, List<List<Double>> result) {
        if (temp.size() == size) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (double scale : scales) {
            temp.add(scale);
            backtrackPorsi(scales, size, temp, result);
            temp.remove(temp.size() - 1);
        }
    }


    private List<List<Map<String, String>>> generateCombinations(List<Map<String, String>> foods, int k) {
        List<List<Map<String, String>>> result = new ArrayList<>();
        backtrackCombinations(foods, k, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrackCombinations(List<Map<String, String>> foods, int k, int start, List<Map<String, String>> temp, List<List<Map<String, String>>> result) {
        if (temp.size() == k) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i < foods.size(); i++) {
            temp.add(foods.get(i));
            backtrackCombinations(foods, k, i + 1, temp, result);
            temp.remove(temp.size() - 1);
        }
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Map<String, Object> evaluateTotalNutrisi(Map<String, Object> rekomendasi) {
        double kebutuhanProtein = (double) rekomendasi.get("kebutuhanProtein");
        double kebutuhanKarbo = (double) rekomendasi.get("kebutuhanKarbohidrat");
        double kebutuhanLemak = (double) rekomendasi.get("kebutuhanLemak");

        double totalProtein = 0.0;
        double totalKarbo = 0.0;
        double totalLemak = 0.0;
        double totalKalori = 0.0;

        String[] waktuMakan = {"Sarapan", "MakanSiang", "MakanMalam"};
        String[] kategori = {"MakananPokok", "Buah", "Sayur", "Lauk"};

        for (String waktu : waktuMakan) {
            Map<String, List<Map<String, String>>> meal = (Map<String, List<Map<String, String>>>) rekomendasi.get(waktu);
            for (String group : kategori) {
                List<Map<String, String>> makanan = meal.get(group);
                if (makanan == null) continue;

                for (Map<String, String> item : makanan) {
                    double scale = parseDoubleSafe(item.get("porsi")) / 100.0;
                    totalProtein += parseDoubleSafe(item.get("protein")) * scale;
                    totalKarbo += parseDoubleSafe(item.get("karbo")) * scale;
                    totalLemak += parseDoubleSafe(item.get("lemak")) * scale;
                    totalKalori += parseDoubleSafe(item.get("kalori")) * scale;
                }
            }
        }

        // Cemilan
        List<Map<String, String>> cemilan = (List<Map<String, String>>) rekomendasi.get("Cemilan");
        if (cemilan != null) {
            for (Map<String, String> item : cemilan) {
                double scale = parseDoubleSafe(item.get("porsi")) / 100.0;
                totalProtein += parseDoubleSafe(item.get("protein")) * scale;
                totalKarbo += parseDoubleSafe(item.get("karbo")) * scale;
                totalLemak += parseDoubleSafe(item.get("lemak")) * scale;
                totalKalori += parseDoubleSafe(item.get("kalori")) * scale;
            }
        }

        double persenProtein = (totalProtein / kebutuhanProtein) * 100.0;
        double persenKarbo = (totalKarbo / kebutuhanKarbo) * 100.0;
        double persenLemak = (totalLemak / kebutuhanLemak) * 100.0;

        double tee = (double) rekomendasi.get("tee");
        double selisihKalori = tee - totalKalori;
        double persenKalori = (totalKalori / tee) * 100.0;

        Map<String, Object> evaluasi = new LinkedHashMap<>();
        evaluasi.put("totalProtein", round(totalProtein));
        evaluasi.put("totalKarbohidrat", round(totalKarbo));
        evaluasi.put("totalLemak", round(totalLemak));
        evaluasi.put("persenProtein", round(persenProtein));
        evaluasi.put("persenKarbohidrat", round(persenKarbo));
        evaluasi.put("persenLemak", round(persenLemak));
        evaluasi.put("totalKalori", round(totalKalori));
        evaluasi.put("persenKalori", round(persenKalori));
        evaluasi.put("selisihKalori", round(selisihKalori));

        evaluasi.put("saran", generateSaran(round(persenProtein), round(persenKarbo), round(persenLemak)));

        return evaluasi;
    }


    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String generateSaran(double persenProtein, double persenKarbo, double persenLemak) {
        List<String> saranList = new ArrayList<>();

        if (persenProtein < 95) saranList.add("Tingkatkan konsumsi lauk tinggi protein seperti ikan, ayam, atau telur.");
        if (persenKarbo < 95) saranList.add("Tambahkan makanan pokok seperti nasi merah, jagung, atau umbi untuk mencukupi karbohidrat.");
        if (persenLemak < 95) saranList.add("Pertimbangkan menambah lemak sehat seperti alpukat, ikan berlemak, atau santan.");

        if (saranList.isEmpty()) {
            return "Asupan harian Anda sudah mencukupi kebutuhan gizi utama. Pertahankan pola makan ini.";
        } else {
            return String.join(" ", saranList);
        }
    }
}
