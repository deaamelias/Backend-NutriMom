package com.recommender.nutrimom.util;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.FileInputStream;
import java.util.*;

public class OWLUtil {

    private static final String OWL_FILE = "src/main/resources/foodontology.owl";
    private static final String BASE_IRI = "http://www.semanticweb.org/dell/ontologies/2025/1/food#";

    private static Model model;

    static {
        try {
            model = ModelFactory.createDefaultModel();
            model.read(new FileInputStream(OWL_FILE), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<Map<String, String>> getRecommendedFoods(String type, Set<String> alergi, boolean diabetesGestasional) {
        Set<Map<String, String>> results = new HashSet<>();

        String diabetesCondition = diabetesGestasional
                ? "FILTER EXISTS { ?item food:memilikiLevelNutrisi food:RendahKarbohidrat }"
                : "";

        String alergiCondition = alergi.isEmpty() ? "" : formatAlergi(alergi);

        // Tambahkan pengambilan data protein, karbohidrat, dan lemak
        String query = String.format(
                "PREFIX food: <%s> " +
                        "SELECT ?name ?calorie ?protein ?carb ?fat " +
                        "WHERE { " +
                        "    ?item a food:%s ; " +
                        "          food:memilikiNama ?name ; " +
                        "          food:memilikiKalori ?calorie ; " +
                        "          food:memilikiProtein ?protein ; " +
                        "          food:memilikiKarbohidrat ?carb ; " +
                        "          food:memilikiLemak ?fat . " +
                        "    %s " + // Kondisi diabetes
                        "    %s " + // Kondisi alergi
                        "}", BASE_IRI, type, diabetesCondition, alergiCondition);

        try (QueryExecution qe = QueryExecutionFactory.create(QueryFactory.create(query), model)) {
            ResultSet resultSet = qe.execSelect();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                String name = solution.getLiteral("name").getString();
                String calorie = solution.getLiteral("calorie").getString();
                String protein = solution.getLiteral("protein").getString();
                String carb = solution.getLiteral("carb").getString();
                String fat = solution.getLiteral("fat").getString();

                Map<String, String> food = new HashMap<>();
                food.put("nama", name);
                food.put("kalori", calorie);
                food.put("protein", protein);
                food.put("karbo", carb);
                food.put("lemak", fat);

                results.add(food);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    private static String formatAlergi(Set<String> alergi) {
        StringBuilder sb = new StringBuilder();
        sb.append("FILTER EXISTS { ");
        for (String allergen : alergi) {
            sb.append(String.format("?item food:tidakMengandung food:%s . ", allergen));
        }
        sb.append("} ");
        return sb.toString();
    }
}

