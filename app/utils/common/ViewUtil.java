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
import java.util.HashMap;
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
    Map<String, Object> fieldValidation = new HashMap<>();

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
        }
      }

      return javaMapToJSON(fieldValidation);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Unable to read validation constraints from form field: " + field.name(), e);
    }
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

}
