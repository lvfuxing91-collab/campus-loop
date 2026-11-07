package edu.campusloop.dao.consistency;

/**
 * 一致性检查发现的一处问题。
 * <p>
 * {@code rule} 是机器可读的规则代号，便于测试与统计；{@code message} 是可读描述；
 * {@code entityId} 指向受影响的业务记录，便于定位。
 */
public final class ConsistencyIssue {

    private final String rule;
    private final String message;
    private final long entityId;

    public ConsistencyIssue(String rule, String message, long entityId) {
        this.rule = rule;
        this.message = message;
        this.entityId = entityId;
    }

    public String getRule() {
        return rule;
    }

    public String getMessage() {
        return message;
    }

    public long getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return "[" + rule + "] " + message + "（记录ID=" + entityId + "）";
    }
}
