package com.recommender.nutrimom.util;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.FileInputStream;
import java.util.*;

public class OWLUtil {

    private static final String OWL_FILE = "src/main/resources/foodrecommender.owl";
    private static final String BASE_IRI = "http://www.semanticweb.org/apple/ontologies/2025/1/food#";

    private static Model model;

    static {
        try {
            model = ModelFactory.createDefaultModel();
            InputStream inputStream = OWLUtil.class.getClassLoader().getResourceAsStream(OWL_FILE);
            if (inputStream == null) {
                throw new FileNotFoundException("❌ File OWL tidak ditemukan di classpath!");
            }
            model.read(inputStream, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<Map<String, String>> getRecommendedFoods(String type, Set<String> alergi) {
        Set<Map<String, String>> results = new HashSet<>();

        // Bersihkan input alergi dari kata "Tidak", "tidak ada", dll
        Set<String> filteredAlergi = new HashSet<>();
        if (alergi != null) {
            for (String item : alergi) {
                String value = item.trim().toLowerCase();
                if (!value.equals("tidak") && !value.equals("tidak ada") && !value.equals("none") && !value.equals("tidak memiliki")) {
                    filteredAlergi.add(item);
                }
            }
        }

        String alergiCondition = filteredAlergi.isEmpty() ? "" : formatAlergi(filteredAlergi);

        String query = String.format(
                "PREFIX food: <%s> " +
                        "SELECT ?name ?calorie ?protein ?carb ?fat ?porsi " +
                        "WHERE { " +
                        "    ?item a food:%s ; " +
                        "          food:memilikiNama ?name ; " +
                        "          food:memilikiKalori ?calorie ; " +
                        "          food:memilikiProtein ?protein ; " +
                        "          food:memilikiKarbohidrat ?carb ; " +
                        "          food:memilikiLemak ?fat ; " +
                        "          food:memilikiPorsi ?porsi . " +
                        "    %s " +
                        "}", BASE_IRI, type, alergiCondition);

        try (QueryExecution qe = QueryExecutionFactory.create(QueryFactory.create(query), model)) {
            ResultSet resultSet = qe.execSelect();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();

                Map<String, String> food = new HashMap<>();
                food.put("nama", solution.getLiteral("name").getString());
                food.put("kalori", solution.getLiteral("calorie").getString());
                food.put("protein", solution.getLiteral("protein").getString());
                food.put("karbo", solution.getLiteral("carb").getString());
                food.put("lemak", solution.getLiteral("fat").getString());
                food.put("porsi", solution.getLiteral("porsi").getString());

                results.add(food);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    // Menghasilkan FILTER EXISTS untuk setiap allergen yang harus dinyatakan secara eksplisit tidak terkandung
    private static String formatAlergi(Set<String> alergi) {
        StringBuilder sb = new StringBuilder();
        for (String allergen : alergi) {
            sb.append(String.format("FILTER EXISTS { ?item food:tidakMengandung food:%s } . ", allergen));
        }
        return sb.toString();
    }
}
