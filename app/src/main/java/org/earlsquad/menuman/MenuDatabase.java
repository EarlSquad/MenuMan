package org.earlsquad.menuman;

import android.content.Context;

import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MenuDatabase {

  private Context context;
  public List<MenuItem> menu = new ArrayList<>();
  public MenuDatabase(Context context) {
    this.context = context;
    AssetManager assets = context.getAssets();
    try {
      InputStream inputStream = assets.open("database/final_menu.csv");
      CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
      String[] nextLine;
      while ((nextLine = reader.readNext()) != null) {
        // nextLine[] is an array of values from the line
        menu.add(new MenuItem(nextLine[1], nextLine[2], nextLine[3], nextLine[4], nextLine[5], nextLine[6], nextLine[7], nextLine[8], nextLine[9], nextLine[10], nextLine[11]));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public List<MenuItem> readFromCSV(String filename) {
    List<MenuItem> menu = new ArrayList<>();
    AssetManager assets = context.getAssets();
    try {
      InputStream inputStream = assets.open(filename);
      CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
      String[] nextLine;
      while ((nextLine = reader.readNext()) != null) {
        // nextLine[] is an array of values from the line
        menu.add(new MenuItem(nextLine[1], nextLine[2], nextLine[3], nextLine[4], nextLine[5], nextLine[6], nextLine[7], nextLine[8], nextLine[9], nextLine[10], nextLine[11]));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return menu;
  }

  public MenuItem search(String word) {
    MenuItem ans = null;
    int mindist = 1000;
    for(MenuItem item : menu) {
      int difference = calculateDist(item.getRealName(), word);
      if(difference < mindist) {
        mindist = difference;
        ans = item;
      }
    }
    if(mindist < 5) {
      return ans;
    } else {
      return null;
    }
  }

  private int calculateDist(String x, String y) {
    int[][] dp = new int[x.length() + 1][y.length() + 1];

    for (int i = 0; i <= x.length(); i++) {
      for (int j = 0; j <= y.length(); j++) {
        if (i == 0) {
          dp[i][j] = j;
        } else if (j == 0) {
          dp[i][j] = i;
        } else {
          dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1), dp[i - 1][j] + 1), dp[i][j - 1] + 1);
        }
      }
    }
    return dp[x.length()][y.length()];
  }

  public void test() {
    MenuItem result = search("CAVIAR DE SOLOGNE");
    System.out.println(result);
  }
}
