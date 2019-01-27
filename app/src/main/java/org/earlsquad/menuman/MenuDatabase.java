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
  }

  public MenuDatabase(String filename) {
    AssetManager assets = context.getAssets();
    try {
      InputStream inputStream = assets.open(filename);
      CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
      String[] nextLine;
      while ((nextLine = reader.readNext()) != null) {
        // nextLine[] is an array of values from the line
        menu.add(new MenuItem(nextLine[0], nextLine[1], nextLine[2], nextLine[3]));
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
        menu.add(new MenuItem(nextLine[0], nextLine[1], nextLine[2], nextLine[3]));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return menu;
  }

  public MenuItem search(String word, List<MenuItem> menu) {
    return menu.get(0);
  }
}
