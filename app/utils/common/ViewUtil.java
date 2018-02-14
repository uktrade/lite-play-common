package utils.common;

import static play.mvc.Controller.ctx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import components.common.journey.BackLink;
import components.common.state.ContextParamManager;
import play.data.Form;
import play.data.validation.Constraints;
import play.mvc.Call;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ViewUtil {

  private static final String BACK_LINK_CONTEXT_PARAM_NAME = "back_link";

  static class DummyMessages {
    public String get(String str){
      return "dummy";
    }

    public String get(String s1, Object o2, Object o3) {
      return "dummy";
    }
  }

  private static final ValidationUtil.DummyMessages Messages = new ValidationUtil.DummyMessages();

  public static ContextParamManager currentParamManager() {
    return (ContextParamManager) ctx().args.get(ContextParamManager.CTX_PARAM_NAME);
  }

  public static String urlFor(Call call) {
    return currentParamManager().addParamsToCall(call);
  }

  public static String unencodedUrlFor(Call call) {
    String url = "";
    try {
      url = URLDecoder.decode(urlFor(call), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // This exception is only thrown if the length of the second param to decode() is 0, and we hardcode it above
    }
    return url;
  }

  /**
   * Create field validation information JSON for a given Form Field
   *
   * @param field Form Field that may have validation conditions
   * @return Null, if no validation conditions for the field or some JSON describing the validation required
   */
  public static String fieldValidationJSON(Form.Field field) {
    Map<String, Object> fieldValidation = new HashMap<>();

    // Reflect through field.form.backedType to get the original Java form class
    Form form = getField(field, "form");
    if (form != null) {
      Class<Object> backedTypeClass = getField(form, "backedType");
      if (backedTypeClass != null) {
        // Get the field from the backed type class for the form field parameter
        String formFieldName = field.name();
        // Remove the array syntax suffix for collection based fields (e.g. "countrySelect[0] -> countrySelect")
        formFieldName = formFieldName.replaceAll("\\[[0-9]+]", "");
        try {
          Field f = backedTypeClass.getDeclaredField(formFieldName);
          if (f != null) {
            // For the various annotation classes that could be applied to the field for validation populate a map with information describing the validation condition
            Constraints.Required required = f.getAnnotation(Constraints.Required.class);
            if (required != null) {
              fieldValidation.putAll(ValidationUtil.getRequiredValidationMap(Messages.get(required.message())));
            }

            Constraints.Email email = f.getAnnotation(Constraints.Email.class);
            if (email != null) {
              fieldValidation.putAll(ValidationUtil.getEmailValidationMap(Messages.get(email.message())));
            }

            Constraints.Pattern pattern = f.getAnnotation(Constraints.Pattern.class);
            if (pattern != null) {
              fieldValidation.putAll(ValidationUtil.getPatternValidationMap(Messages.get(pattern.message())));
            }

            Constraints.Max max = f.getAnnotation(Constraints.Max.class);
            if (max != null) {
              fieldValidation.putAll(ValidationUtil.getMaxValidationMap(Messages.get(max.message()), max.value()));
            }

            Constraints.Min min = f.getAnnotation(Constraints.Min.class);
            if (min != null) {
              fieldValidation.putAll(ValidationUtil.getMinValidationMap(Messages.get(min.message()), min.value()));
            }

            Constraints.MaxLength maxLength = f.getAnnotation(Constraints.MaxLength.class);
            if (maxLength != null) {
              fieldValidation.putAll(ValidationUtil.getMaxLengthValidationMap(Messages.get(maxLength.message()), maxLength.value()));
            }

            Constraints.MinLength minLength = f.getAnnotation(Constraints.MinLength.class);
            if (minLength != null) {
              fieldValidation.putAll(ValidationUtil.getMinLengthValidationMap(Messages.get(minLength.message()), minLength.value()));
            }
          }
        } catch (NoSuchFieldException e) {
          throw new RuntimeException("Unable to read validation constraints from form field: " + formFieldName, e);
        }
      }
    }

    return javaMapToJSON(fieldValidation);
  }

  /**
   * Convert a java map to a JSON object as a string
   *
   * @param map java map to convert to a JSON object
   * @return String representation of a JSON object with members and values from the map parameter
   */
  public static String javaMapToJSON(Map map) {
    try {
      return new ObjectMapper().writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to process map to JSON String", e);
    }
  }


  private static <T> T getField(Object container, String fieldName) {
    try {
      Field field = container.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return (T)field.get(container);
    }
    catch (Exception e) {
      return null;
    }
  }

  public static void overrideBackLink(BackLink backLink) {
    ctx().args.put(BACK_LINK_CONTEXT_PARAM_NAME, backLink);
  }

  public static Optional<BackLink> currentBackLink() {
    return Optional.ofNullable((BackLink) ctx().args.get(BACK_LINK_CONTEXT_PARAM_NAME));
  }

  /**
   * Pluralises a word by appending an 's' if count is not zero
   *
   * @param count The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @return String The plural of the word if count is not 1, otherwise the singular
   */
  public static String pluralise(Number count, String singular) {
    return pluralise(count, singular, singular + 's');
  }

  /**
   * Pluralises a word by using the supplied plural form if count is not zero
   *
   * @param count The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @param plural The plural form of the word
   * @return String The plural of the word if count is not 1, otherwise the singular
   */
  public static String pluralise(Number count, String singular, String plural) {
    if(count.equals(1)) {
      return singular;
    }
    else {
      return plural;
    }
  }

  /**
   * Returns the count and the plural of a word by appending an 's' if count is not zero
   *
   * @param count The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @return String The count, and the plural of the word if count is not 1, otherwise the singular
   */
  public static String pluraliseWithCount(Number count, String singular) {
    return count + " " + pluralise(count, singular);
  }

  /**
   * Returns the count and the plural of a word by using the supplied plural form if count is not zero
   *
   * @param count The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @param plural The plural form of the word
   * @return String The count, and the plural of the word if count is not 1, otherwise the singular
   */
  public static String pluraliseWithCount(Number count, String singular, String plural) {
    return count + " " + pluralise(count, singular, plural);
  }

}
