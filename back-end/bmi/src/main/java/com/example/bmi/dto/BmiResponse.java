package com.example.bmi.dto;

public class BmiResponse {

    private Double bmi;
    private String category;
    private String message;
    private Boolean saved;

    public BmiResponse(
            Double bmi,
            String category,
            String message,
            Boolean saved
    ) {
        this.bmi = bmi;
        this.category = category;
        this.message = message;
        this.saved = saved;
    }

    public Double getBmi() {
        return bmi;
    }

    public String getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public Boolean getSaved() {
        return saved;
    }
}
