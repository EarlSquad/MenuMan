package org.earlsquad.menuman;

import android.content.Context;

import android.content.res.AssetManager;
import android.util.Log;
import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ReadCSV {

  private Context context;

  public ReadCSV(Context context) {
    this.context = context;
  }

  public void test() {
    AssetManager assets = context.getAssets();
    try {
      InputStream inputStream = assets.open("database/german.csv");
      CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
      String[] nextLine;
      while ((nextLine = reader.readNext()) != null) {
        // nextLine[] is an array of values from the line
        Log.d("CSV", nextLine[0] + nextLine[1] + "etc...");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
