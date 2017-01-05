package utils.common;

import static play.mvc.Controller.ctx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import components.common.journey.BackLink;
import components.common.state.ContextParamManager;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Call;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ViewUtil {

  private static final String BACK_LINK_CONTEXT_PARAM_NAME = "back_link";

  public static ContextParamManager currentParamManager() {
    return (ContextParamManager) ctx().args.get(ContextParamManager.CTX_PARAM_NAME);
  }

  public static String urlFor(Call call) {
    return currentParamManager().addParamsToCall(call);
  }

  /**
   * Create field validation information JSON for a given Form Field
   *
   * @param field Form Field that may have validation conditions
   * @return Null, if no validation conditions for the field or some JSON describing the validation required
   */
  public static String fieldValidationJSON(Form.Field field) {
    List<String> fieldValidation = new ArrayList<>();

    try {
      // Reflect through field.form.backedType to get the original Java form class
      Form form = getField(field, "form");
      if (form != null) {
        Class<Object> backedTypeClass = getField(form, "backedType");
        if (backedTypeClass != null) {
          // Get the field from the backed type class for the form field parameter
          Field f = backedTypeClass.getDeclaredField(field.name());
          if (f != null) {
            // For the various annotation classes that could be applied to the field for validation populate a map with information describing the validation condition
            Constraints.Required required = f.getAnnotation(Constraints.Required.class);
            if (required != null) {
              fieldValidation.add(getRequiredValidationJSON(Messages.get(required.message())));
            }

            Constraints.Email email = f.getAnnotation(Constraints.Email.class);
            if (email != null) {
              fieldValidation.add(getEmailValidationJSON(Messages.get(email.message())));
            }

            Constraints.Pattern pattern = f.getAnnotation(Constraints.Pattern.class);
            if (pattern != null) {
              fieldValidation.add(getPatternValidationJSON(Messages.get(pattern.message())));
            }

            Constraints.Max max = f.getAnnotation(Constraints.Max.class);
            if (max != null) {
              fieldValidation.add(getMaxValidationJSON(Messages.get(max.message()), max.value()));
            }

            Constraints.Min min = f.getAnnotation(Constraints.Min.class);
            if (min != null) {
              fieldValidation.add(getMinValidationJSON(Messages.get(min.message()), min.value()));
            }

            Constraints.MaxLength maxLength = f.getAnnotation(Constraints.MaxLength.class);
            if (maxLength != null) {
              fieldValidation.add(getMaxLengthValidationJSON(Messages.get(maxLength.message()), maxLength.value()));
            }

            Constraints.MinLength minLength = f.getAnnotation(Constraints.MinLength.class);
            if (minLength != null) {
              fieldValidation.add(getMinLengthValidationJSON(Messages.get(minLength.message()), minLength.value()));
            }
          }
        }
      }

      return "[" + String.join(",", fieldValidation) + "]";
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String getRequiredValidationJSON(String message) {
    if (message == null) {
      message = Messages.get(Constraints.RequiredValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> requiredDetails = new HashMap<>();
    requiredDetails.put("message", Messages.get(message));
    validationData.put("required", requiredDetails);

    return mapToJSONString(validationData);
  }

  public static String getEmailValidationJSON(String message) {
    if (message == null) {
      message = Messages.get(Constraints.EmailValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> emailDetails = new HashMap<>();
    emailDetails.put("message", Messages.get(message));
    validationData.put("email", emailDetails);

    return mapToJSONString(validationData);
  }

  public static String getPatternValidationJSON(String message) {
    if (message == null) {
      message = Messages.get(Constraints.PatternValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> patternDetails = new HashMap<>();
    patternDetails.put("message", Messages.get(message));
    validationData.put("pattern", patternDetails);

    return mapToJSONString(validationData);
  }

  public static String getMaxValidationJSON(String message, long maxValue) {
    if (message == null) {
      message = Messages.get(Constraints.MaxValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> maxDetails = new HashMap<>();
    maxDetails.put("message", Messages.get(message, null, maxValue));
    maxDetails.put("limit", maxValue);
    validationData.put("max", maxDetails);

    return mapToJSONString(validationData);
  }

  public static String getMinValidationJSON(String message, long minValue) {
    if (message == null) {
      message = Messages.get(Constraints.MinValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> minDetails = new HashMap<>();
    minDetails.put("message", Messages.get(message, null, minValue));
    minDetails.put("limit", minValue);
    validationData.put("min", minDetails);

    return mapToJSONString(validationData);
  }

  public static String getMaxLengthValidationJSON(String message, long maxLength) {
    if (message == null) {
      message = Messages.get(Constraints.MaxLengthValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> maxLengthDetails = new HashMap<>();
    maxLengthDetails.put("message", Messages.get(message, null, maxLength));
    maxLengthDetails.put("limit", maxLength);
    validationData.put("max", maxLengthDetails);

    return mapToJSONString(validationData);
  }

  public static String getMinLengthValidationJSON(String message, long minLength) {
    if (message == null) {
      message = Messages.get(Constraints.MinLengthValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> minLengthDetails = new HashMap<>();
    minLengthDetails.put("message", Messages.get(message, null, minLength));
    minLengthDetails.put("limit", minLength);
    validationData.put("min", minLengthDetails);

    return mapToJSONString(validationData);
  }

  private static String mapToJSONString(Map map) {
    try {
      return new ObjectMapper().writeValueAsString(map);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
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

}
