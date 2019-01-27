package org.earlsquad.menuman;

public class MenuItem {
    private String realName;
    private String engName;
    private String imageURL1;
    private String imageURL2;
    private String ingredients;
    private String allergies;
    private String calories;
    private String carbs;
    private String fats;
    private String proteins;
    private String fact;

    public String getIngredients() {
        return ingredients;
    }

    public String getAllergies() {
        return allergies;
    }

    public String getCalories() {
        return calories;
    }

    public String getCarbs() {
        return carbs;
    }

    public String getFats() {
        return fats;
    }

    public String getProteins() {
        return proteins;
    }

    public String getFact() {
        return fact;
    }

    public MenuItem(String realName, String engName, String imageURL1, String imageURL2, String ingredients, String allergies, String calories, String carbs, String fats, String proteins, String fact) {
        this.realName = realName;
        this.engName = engName;
        this.imageURL1 = imageURL1;
        this.imageURL2 = imageURL2;
        this.ingredients = ingredients;
        this.allergies = allergies;
        this.calories = calories;
        this.carbs = carbs;
        this.fats = fats;
        this.proteins = proteins;
        this.fact = fact;
    }

    public String getRealName() {
        return realName;
    }

    public String getEngName() {
        return engName;
    }

    public String getImageURL1() {
        return imageURL1;
    }

    public String getImageURL2() {
        return imageURL2;
    }

    @Override
    public String toString() {
        return "MenuItem{" +
            "realName='" + realName + '\'' +
            ", engName='" + engName + '\'' +
            '}';
    }
}
