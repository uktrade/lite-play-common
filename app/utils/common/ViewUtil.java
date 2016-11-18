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
   * Create field validation information JSON
   *
   * @param field Form Field that may have validation conditions
   * @return Null, if no validation conditions for the field or some JSON describing the validation required
   */
  public static String fieldValidationJSON(Form.Field field) {
    Map<String, Object> validationData = new HashMap<>();

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
              Map<String, Object> requiredDetails = new HashMap<>();
              requiredDetails.put("message", Messages.get(required.message()));

              validationData.put("required", requiredDetails);
            }

            Constraints.Email email = f.getAnnotation(Constraints.Email.class);
            if (email != null) {
              Map<String, Object> emailDetails = new HashMap<>();
              emailDetails.put("message", Messages.get(email.message()));

              validationData.put("email", emailDetails);
            }

            Constraints.Pattern pattern = f.getAnnotation(Constraints.Pattern.class);
            if (pattern != null) {
              Map<String, Object> patternDetails = new HashMap<>();
              patternDetails.put("message", Messages.get(pattern.message()));
              patternDetails.put("pattern", pattern.value());

              validationData.put("pattern", patternDetails);
            }

            Constraints.Max max = f.getAnnotation(Constraints.Max.class);
            if (max != null) {
              Map<String, Object> maxDetails = new HashMap<>();
              maxDetails.put("message", Messages.get(max.message(), null, max.value()));
              maxDetails.put("limit", max.value());

              validationData.put("max", maxDetails);
            }

            Constraints.Min min = f.getAnnotation(Constraints.Min.class);
            if (min != null) {
              Map<String, Object> minDetails = new HashMap<>();
              minDetails.put("message", Messages.get(min.message(), null, min.value()));
              minDetails.put("limit", min.value());

              validationData.put("min", minDetails);
            }

            Constraints.MaxLength maxLength = f.getAnnotation(Constraints.MaxLength.class);
            if (maxLength != null) {
              Map<String, Object> maxLengthDetails = new HashMap<>();
              maxLengthDetails.put("message", Messages.get(maxLength.message(), null, maxLength.value()));
              maxLengthDetails.put("limit", maxLength.value());

              validationData.put("maxLength", maxLengthDetails);
            }

            Constraints.MinLength minLength = f.getAnnotation(Constraints.MinLength.class);
            if (minLength != null) {
              Map<String, Object> minLengthDetails = new HashMap<>();
              minLengthDetails.put("message", Messages.get(minLength.message(), null, minLength.value()));
              minLengthDetails.put("limit", minLength.value());

              validationData.put("minLength", minLengthDetails);
            }
          }
        }
      }

      try {
        if (validationData.size() > 0) {
          return new ObjectMapper().writeValueAsString(validationData);
        }
        else {
          return null;
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        return null;
      }
    } catch (Exception e) {
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
