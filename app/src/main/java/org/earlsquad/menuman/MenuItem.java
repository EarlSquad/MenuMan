package org.earlsquad.menuman;

public class MenuItem {
    private String realName;
    private String engName;
    private String imageURL1;
    private String imageURL2;

    public MenuItem(String realName, String engName, String imageURL1, String imageURL2) {
        this.realName = realName;
        this.engName = engName;
        this.imageURL1 = imageURL1;
        this.imageURL2 = imageURL2;
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
}
