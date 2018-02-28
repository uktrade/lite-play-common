package utils.common;

import play.data.validation.Constraints;
import play.i18n.Messages;

import java.util.HashMap;
import java.util.Map;

public class ValidationUtil {

  public static Map<String, Object> getRequiredValidationMap(String message) {
    if (message == null) {
      message = Messages.get(Constraints.RequiredValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> requiredDetails = new HashMap<>();
    requiredDetails.put("message", Messages.get(message));
    validationData.put("required", requiredDetails);

    return validationData;
  }

  public static Map<String, Object> getEmailValidationMap(String message) {
    if (message == null) {
      message = Messages.get(Constraints.EmailValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> emailDetails = new HashMap<>();
    emailDetails.put("message", Messages.get(message));
    validationData.put("email", emailDetails);

    return validationData;
  }

  public static Map<String, Object> getPatternValidationMap(String message) {
    if (message == null) {
      message = Messages.get(Constraints.PatternValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> patternDetails = new HashMap<>();
    patternDetails.put("message", Messages.get(message));
    validationData.put("pattern", patternDetails);

    return validationData;
  }

  public static Map<String, Object> getMaxValidationMap(String message, long maxValue) {
    if (message == null) {
      message = Messages.get(Constraints.MaxValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> maxDetails = new HashMap<>();
    maxDetails.put("message", Messages.get(message, null, maxValue));
    maxDetails.put("limit", maxValue);
    validationData.put("max", maxDetails);

    return validationData;
  }

  public static Map<String, Object> getMinValidationMap(String message, long minValue) {
    if (message == null) {
      message = Messages.get(Constraints.MinValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> minDetails = new HashMap<>();
    minDetails.put("message", Messages.get(message, null, minValue));
    minDetails.put("limit", minValue);
    validationData.put("min", minDetails);

    return validationData;
  }

  public static Map<String, Object> getMaxLengthValidationMap(String message, long maxLength) {
    if (message == null) {
      message = Messages.get(Constraints.MaxLengthValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> maxLengthDetails = new HashMap<>();
    maxLengthDetails.put("message", Messages.get(message, null, maxLength));
    maxLengthDetails.put("limit", maxLength);
    validationData.put("maxLength", maxLengthDetails);

    return validationData;
  }

  public static Map<String, Object> getMinLengthValidationMap(String message, long minLength) {
    if (message == null) {
      message = Messages.get(Constraints.MinLengthValidator.message);
    }
    Map<String, Object> validationData = new HashMap<>();
    Map<String, Object> minLengthDetails = new HashMap<>();
    minLengthDetails.put("message", Messages.get(message, null, minLength));
    minLengthDetails.put("limit", minLength);
    validationData.put("minLength", minLengthDetails);

    return validationData;
  }

}
