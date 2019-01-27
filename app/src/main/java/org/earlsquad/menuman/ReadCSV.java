package org.earlsquad.menuman;

import android.content.Context;

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

    static public void test() {

        try {

            CSVReader reader = new CSVReader(new FileReader("german.csv"));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                System.out.println(nextLine[0] + nextLine[1] + "etc...");
            }
        } catch (IOException e) {
            System.out.println("hello");
        }
    }


    public static void main(String[] args) {
        test();

    }

}
