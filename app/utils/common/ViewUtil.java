package utils.common;

import static play.mvc.Controller.ctx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import components.common.CommonContextActionSetup;
import components.common.journey.BackLink;
import components.common.state.ContextParamManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import play.data.Form;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.mvc.Call;
import play.mvc.Http;
import uk.gov.bis.lite.countryservice.api.CountryView;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ViewUtil {

  private static final String BACK_LINK_CONTEXT_PARAM_NAME = "back_link";
  private static final ObjectMapper MAPPER = new ObjectMapper();

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

  public static String formatError(ValidationError validationError) {
    return validationError.format(messagesApi().preferred(Http.Context.current().request().acceptLanguages()));
  }

  /**
   * Create field validation information JSON for a given Form Field
   *
   * @param field Form Field that may have validation conditions
   * @return Null, if no validation conditions for the field or some JSON describing the validation required
   */
  public static String fieldValidationJSON(Form.Field field) {
    // Reflect through field.form.backedType to get the original Java form class
    Form form = getField(field, "form");
    if (form != null) {
      Class backedTypeClass = getField(form, "backedType");
      if (backedTypeClass != null) {
        // Get the field from the backed type class for the form field parameter
        Optional<String> name = field.getName();
        if (name.isPresent()) {
          // Remove the array syntax suffix for collection based fields (e.g. "countrySelect[0] -> countrySelect")
          String normalized = name.get().replaceAll("\\[[0-9]+]", "");
          Field f = getField(backedTypeClass, normalized);
          Map<String, Object> validationMap = getValidationMap(f);
          return convertMapToJson(validationMap);
        }
      }
    }
    return convertMapToJson(new HashMap());
  }

  @SuppressWarnings("unchecked")
  private static <T> T getField(Object container, String fieldName) {
    try {
      Field field = container.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return (T) field.get(container);
    } catch (Exception e) {
      return null;
    }
  }

  private static Field getField(Class clazz, String name) {
    try {
      return clazz.getDeclaredField(name);
    } catch (NoSuchFieldException exception) {
      throw new RuntimeException("Unable to read validation constraints from form field: " + name, exception);
    }
  }

  public static Map<String, Object> getRequiredValidationMap() {
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> requiredDetails = new HashMap<>();
    requiredDetails.put("message", getMessage(Constraints.RequiredValidator.message));
    validationData.put("required", requiredDetails);
    return validationData;
  }

  private static Map<String, Object> getValidationMap(Field field) {
    Map<String, Object> validation = new HashMap<>();

    // For the various annotation classes that could be applied to the field for validation populate a map with information describing the validation condition
    Constraints.Required required = field.getAnnotation(Constraints.Required.class);
    if (required != null) {
      validation.putAll(getValidationData(required.message(), Constraints.RequiredValidator.message));
    }

    Constraints.Email email = field.getAnnotation(Constraints.Email.class);
    if (email != null) {
      validation.putAll(getValidationData(email.message(), Constraints.EmailValidator.message));
    }

    Constraints.Pattern pattern = field.getAnnotation(Constraints.Pattern.class);
    if (pattern != null) {
      validation.putAll(getValidationData(pattern.message(), Constraints.PatternValidator.message));
    }

    Constraints.Max max = field.getAnnotation(Constraints.Max.class);
    if (max != null) {
      validation.putAll(getValidationDataWithParameter(max.message(), Constraints.MaxValidator.message, max.value()));
    }

    Constraints.Min min = field.getAnnotation(Constraints.Min.class);
    if (min != null) {
      validation.putAll(getValidationDataWithParameter(min.message(), Constraints.MinValidator.message, min.value()));
    }

    Constraints.MaxLength maxLength = field.getAnnotation(Constraints.MaxLength.class);
    if (maxLength != null) {
      validation.putAll(getValidationDataWithParameter(maxLength.message(), Constraints.MaxLengthValidator.message, maxLength.value()));
    }

    Constraints.MinLength minLength = field.getAnnotation(Constraints.MinLength.class);
    if (minLength != null) {
      validation.putAll(getValidationDataWithParameter(minLength.message(), Constraints.MinLengthValidator.message, minLength.value()));
    }
    return validation;
  }

  private static MessagesApi messagesApi() {
    return (MessagesApi) ctx().args.get(CommonContextActionSetup.CTX_MESSAGE_API_NAME);
  }

  private static String getMessage(String message, Object... args) {
    return messagesApi().get(Lang.defaultLang(), message, args);
  }

  private static String translate(String message, String defaultMessage, Object... args) {
    String translatedMessage = getMessage(message, args);
    if (message == null) {
      return getMessage(defaultMessage, args);
    } else {
      return translatedMessage;
    }
  }

  private static Map<String, Map<String, Object>> getValidationData(String message, String constraint) {
    Map<String, Map<String, Object>> validationData = new HashMap<>();
    Map<String, Object> requiredDetails = new HashMap<>();
    requiredDetails.put("message", translate(message, constraint));
    String constraintName = StringUtils.removeStart(constraint, "error.");
    validationData.put(constraintName, requiredDetails);
    return validationData;
  }

  private static Map<String, Map<String, Object>> getValidationDataWithParameter(String message, String constraint, long parameter) {
    Map<String, Map<String, Object>> validationData = new HashMap<>();
    Map<String, Object> minLengthDetails = new HashMap<>();
    minLengthDetails.put("message", translate(message, constraint, null, parameter));
    minLengthDetails.put("limit", parameter);
    String constraintName = StringUtils.removeStart(constraint, "error.");
    validationData.put(constraintName, minLengthDetails);
    return validationData;
  }

  /**
   * Convert a java map to a JSON object as a string
   *
   * @param map java map to convert to a JSON object
   * @return String representation of a JSON object with members and values from the map parameter
   */
  public static String convertMapToJson(Map map) {
    try {
      return MAPPER.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to process map to JSON String", e);
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
   * @param count    The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @return String The plural of the word if count is not 1, otherwise the singular
   */
  public static String pluralise(Number count, String singular) {
    return pluralise(count, singular, singular + 's');
  }

  /**
   * Pluralises a word by using the supplied plural form if count is not zero
   *
   * @param count    The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @param plural   The plural form of the word
   * @return String The plural of the word if count is not 1, otherwise the singular
   */
  public static String pluralise(Number count, String singular, String plural) {
    if (count.intValue() == 1) {
      return singular;
    } else {
      return plural;
    }
  }

  /**
   * Returns the count and the plural of a word by appending an 's' if count is not zero
   *
   * @param count    The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @return String The count, and the plural of the word if count is not 1, otherwise the singular
   */
  public static String pluraliseWithCount(Number count, String singular) {
    return count + " " + pluralise(count, singular);
  }

  /**
   * Returns the count and the plural of a word by using the supplied plural form if count is not zero
   *
   * @param count    The count of things used to decide whether to pluralise the word
   * @param singular The singular form of the word
   * @param plural   The plural form of the word
   * @return String The count, and the plural of the word if count is not 1, otherwise the singular
   */
  public static String pluraliseWithCount(Number count, String singular, String plural) {
    return count + " " + pluralise(count, singular, plural);
  }

  public static String getSynonymsAsString(CountryView countryView) {
    if (CollectionUtils.isEmpty(countryView.getSynonyms())) {
      return "";
    } else {
      return String.join(" ", countryView.getSynonyms());
    }
  }

}
