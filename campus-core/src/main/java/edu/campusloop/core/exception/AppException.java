package edu.campusloop.core.exception;

public class AppException extends RuntimeException {

    /**
     * 仅用于日志的诊断细节，例如具体SQL或文件路径。
     * 该字段不应直接展示给最终用户。
     */
    private final String detail;

    public AppException(String message) {
        this(message, null, null);
    }

    public AppException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public AppException(String message, String detail, Throwable cause) {
        super(message, cause);
        this.detail = detail;
    }

    /** 返回可安全展示给用户的中文消息，等同于 {@link #getMessage()}。 */
    public String getUserMessage() {
        return getMessage();
    }

    /** 返回诊断细节，可能包含SQL、路径等内部信息，仅供日志使用。 */
    public String getDetail() {
        return detail;
    }
}
