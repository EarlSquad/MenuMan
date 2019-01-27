package org.earlsquad.menuman;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;

import java.io.IOException;
import java.util.List;

public class MenuAPI {

  private String apiKey;
  private String cx;

  public MenuAPI(String apiKey, String cx) {
    this.apiKey = apiKey;
    this.cx = cx;
  }

  public String searchThumbnail(String query) throws IOException {
    HttpTransport transport = new ApacheHttpTransport();
    JsonFactory jsonFactory = new JacksonFactory();
    HttpRequestInitializer initializer = new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest request) {

      }
    };
    Customsearch.Builder builder = new Customsearch.Builder(transport, jsonFactory, initializer);
    Customsearch search = builder.build();
    Customsearch.Cse.List list = search.cse().list(query);

    list.setKey(apiKey);
    list.setCx(cx);
    list.setSearchType("image");
    list.setSafe("active");
    list.setNum(1L);

    List<Result> results = list.execute().getItems();
    if (results.size() == 0) {
      return null;
    }
    return results.get(0).getImage().getThumbnailLink();
  }
}
