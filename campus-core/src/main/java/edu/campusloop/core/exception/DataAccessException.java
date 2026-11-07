package edu.campusloop.core.exception;

public class DataAccessException extends AppException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message, String detail, Throwable cause) {
        super(message, detail, cause);
    }
}
