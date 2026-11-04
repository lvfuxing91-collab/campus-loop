package edu.campusloop.core.exception;

public class ValidationException extends AppException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
