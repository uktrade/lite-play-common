package models.common;

import java.util.List;

public class Country {

  private String countryRef;

  private String countryName;

  private List<String> synonyms;

  public String getCountryRef() {
    return countryRef;
  }

  public String getCountryName() {
    return countryName;
  }

  public List<String> getSynonyms() {
    return synonyms;
  }
  
  public String getSynonymsAsString() {
    if (synonyms == null) {
      return "";
    } else {
      return String.join(" ", synonyms);
    }
  }
}
