package org.earlsquad.menuman;

public class MenuItem {
  private String realName;
  private String engName;
  private String imageURL1 = "http://www.bennettig.com/wordpress/wp-content/uploads/2018/07/square-placeholder.jpg";
  private String imageURL2 = "http://www.bennettig.com/wordpress/wp-content/uploads/2018/07/square-placeholder.jpg";
  private String description = "dummy description";

  public MenuItem(String realName, String engName, String imageURL1, String imageURL2) {
    this.realName = realName;
    this.engName = engName;
    if (imageURL1 != null && !imageURL1.isEmpty())
      this.imageURL1 = imageURL1;
    if (imageURL2 != null && !imageURL2.isEmpty())
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

  @Override
  public String toString() {
    return "MenuItem{" + "realName='" + realName + '\'' + ", engName='" + engName + '\'' + '}';
  }

  public String getDescription() {
    return description;
  }
}
