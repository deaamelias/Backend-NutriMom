package com.recommender.nutrimom.model;

import java.util.List;

public class FoodInput {

    private double tinggiBadan;
    private double beratBadan;
    private int usia;
    private int usiaKehamilan;
    private String levelAktivitas;
    private String kategoriStres;
    private String diabetesGestasional;
    private List<String> alergi;

    // Constructor (opsional)
    public FoodInput() {
    }

    // Getter dan Setter
    public double getTinggiBadan() {
        return tinggiBadan;
    }

    public void setTinggiBadan(double tinggiBadan) {
        this.tinggiBadan = tinggiBadan;
    }

    public double getBeratBadan() {
        return beratBadan;
    }

    public void setBeratBadan(double beratBadan) {
        this.beratBadan = beratBadan;
    }

    public int getUsia() {
        return usia;
    }

    public void setUsia(int usia) {
        this.usia = usia;
    }

    public int getUsiaKehamilan() {
        return usiaKehamilan;
    }

    public void setUsiaKehamilan(int usiaKehamilan) {
        this.usiaKehamilan = usiaKehamilan;
    }

    public String getLevelAktivitas() {
        return levelAktivitas;
    }

    public void setLevelAktivitas(String levelAktivitas) {
        this.levelAktivitas = levelAktivitas;
    }

    public String getKategoriStres() {
        return kategoriStres;
    }

    public void setKategoriStres(String kategoriStres) {
        this.kategoriStres = kategoriStres;
    }

    public String getDiabetesGestasional() {
        return diabetesGestasional;
    }

    public void setDiabetesGestasional(String diabetesGestasional) {
        this.diabetesGestasional = diabetesGestasional;
    }

    public List<String> getAlergi() {
        return alergi;
    }

    public void setAlergi(List<String> alergi) {
        this.alergi = alergi;
    }
}
