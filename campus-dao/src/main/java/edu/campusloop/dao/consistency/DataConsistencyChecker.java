package edu.campusloop.dao.consistency;

import edu.campusloop.core.exception.DataAccessException;
import edu.campusloop.dao.db.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库层面的业务一致性检查。
 * <p>
 * 外键、唯一约束和 CHECK 只能保证单表结构合法，无法表达“流转完成时物品也应完成”
 * 这类跨表业务规则。服务层实现后由业务保证；本检查器用于在开发、验收和升级数据时
 * 发现已经存在的不一致，避免错误数据被统计和展示。
 * <p>
 * 检查只读，不修改任何数据。
 */
public final class DataConsistencyChecker {

    /** 流转已完成，但关联物品状态不是已完成。 */
    public static final String DEAL_COMPLETED_ITEM_NOT = "DEAL_COMPLETED_ITEM_NOT";
    /** 物品已完成，但没有任何已完成的流转记录支持这一状态。 */
    public static final String ITEM_COMPLETED_WITHOUT_DEAL = "ITEM_COMPLETED_WITHOUT_DEAL";
    /** 评价挂在未完成的流转上。 */
    public static final String REVIEW_ON_UNFINISHED_DEAL = "REVIEW_ON_UNFINISHED_DEAL";
    /** 评价人或被评价人不是该流转的买卖双方。 */
    public static final String REVIEW_NOT_PARTICIPANT = "REVIEW_NOT_PARTICIPANT";
    /** 买家与卖家是同一个人。 */
    public static final String SELF_DEAL = "SELF_DEAL";
    /** 同一物品存在多条已完成流转。 */
    public static final String MULTIPLE_COMPLETED_DEALS = "MULTIPLE_COMPLETED_DEALS";
    /** 同一物品同时存在多条已接受的流转。 */
    public static final String MULTIPLE_ACCEPTED_DEALS = "MULTIPLE_ACCEPTED_DEALS";
    /** 完成时间与状态不一致。 */
    public static final String COMPLETED_AT_MISMATCH = "COMPLETED_AT_MISMATCH";

    private final DatabaseManager database;

    public DataConsistencyChecker(DatabaseManager database) {
        this.database = database;
    }

    /**
     * 执行全部检查，返回所有发现的问题。空列表表示未发现不一致。
     */
    public List<ConsistencyIssue> checkAll() {
        List<ConsistencyIssue> issues = new ArrayList<>();
        issues.addAll(findDealCompletedButItemNot());
        issues.addAll(findItemCompletedWithoutDeal());
        issues.addAll(findReviewOnUnfinishedDeal());
        issues.addAll(findReviewNotParticipant());
        issues.addAll(findSelfDeal());
        issues.addAll(findMultipleCompletedDeals());
        issues.addAll(findMultipleAcceptedDeals());
        issues.addAll(findCompletedAtMismatch());
        return issues;
    }

    /** 流转已完成，物品却不是 COMPLETED。 */
    List<ConsistencyIssue> findDealCompletedButItemNot() {
        return collect(
                "SELECT d.id, d.item_id, i.status AS item_status"
                        + " FROM deals d JOIN items i ON i.id = d.item_id"
                        + " WHERE d.status = 'COMPLETED' AND i.status <> 'COMPLETED'",
                DEAL_COMPLETED_ITEM_NOT,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(DEAL_COMPLETED_ITEM_NOT,
                                "流转 #" + rs.getLong("id") + " 已完成，但物品 #"
                                        + rs.getLong("item_id") + " 状态为 " + rs.getString("item_status"),
                                rs.getLong("id"));
                    }
                });
    }

    /** 物品已完成，但没有已完成的流转记录。 */
    List<ConsistencyIssue> findItemCompletedWithoutDeal() {
        return collect(
                "SELECT i.id FROM items i"
                        + " WHERE i.status = 'COMPLETED'"
                        + " AND NOT EXISTS (SELECT 1 FROM deals d"
                        + "   WHERE d.item_id = i.id AND d.status = 'COMPLETED')",
                ITEM_COMPLETED_WITHOUT_DEAL,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(ITEM_COMPLETED_WITHOUT_DEAL,
                                "物品 #" + rs.getLong("id") + " 标记为已完成，但缺少已完成的流转记录",
                                rs.getLong("id"));
                    }
                });
    }

    /** 评价挂在未完成的流转上。 */
    List<ConsistencyIssue> findReviewOnUnfinishedDeal() {
        return collect(
                "SELECT r.id, r.deal_id, d.status AS deal_status"
                        + " FROM reviews r JOIN deals d ON d.id = r.deal_id"
                        + " WHERE d.status <> 'COMPLETED'",
                REVIEW_ON_UNFINISHED_DEAL,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(REVIEW_ON_UNFINISHED_DEAL,
                                "评价 #" + rs.getLong("id") + " 所属流转 #" + rs.getLong("deal_id")
                                        + " 状态为 " + rs.getString("deal_status"),
                                rs.getLong("id"));
                    }
                });
    }

    /** 评价人或被评价人不是该流转的买卖双方。 */
    List<ConsistencyIssue> findReviewNotParticipant() {
        return collect(
                "SELECT r.id, r.deal_id FROM reviews r JOIN deals d ON d.id = r.deal_id"
                        + " WHERE NOT ("
                        + "   (r.reviewer_id = d.buyer_id AND r.reviewee_id = d.seller_id)"
                        + "   OR (r.reviewer_id = d.seller_id AND r.reviewee_id = d.buyer_id))",
                REVIEW_NOT_PARTICIPANT,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(REVIEW_NOT_PARTICIPANT,
                                "评价 #" + rs.getLong("id") + " 的双方不是流转 #"
                                        + rs.getLong("deal_id") + " 的买卖双方",
                                rs.getLong("id"));
                    }
                });
    }

    /** 买家与卖家相同。 */
    List<ConsistencyIssue> findSelfDeal() {
        return collect(
                "SELECT id FROM deals WHERE buyer_id = seller_id",
                SELF_DEAL,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(SELF_DEAL,
                                "流转 #" + rs.getLong("id") + " 的买家与卖家相同", rs.getLong("id"));
                    }
                });
    }

    /** 同一物品存在多条已完成流转。 */
    List<ConsistencyIssue> findMultipleCompletedDeals() {
        return collect(
                "SELECT item_id, COUNT(*) AS total FROM deals"
                        + " WHERE status = 'COMPLETED' GROUP BY item_id HAVING COUNT(*) > 1",
                MULTIPLE_COMPLETED_DEALS,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(MULTIPLE_COMPLETED_DEALS,
                                "物品 #" + rs.getLong("item_id") + " 有 "
                                        + rs.getInt("total") + " 条已完成流转",
                                rs.getLong("item_id"));
                    }
                });
    }

    /** 同一物品同时存在多条已接受流转，会导致重复预订。 */
    List<ConsistencyIssue> findMultipleAcceptedDeals() {
        return collect(
                "SELECT item_id, COUNT(*) AS total FROM deals"
                        + " WHERE status = 'ACCEPTED' GROUP BY item_id HAVING COUNT(*) > 1",
                MULTIPLE_ACCEPTED_DEALS,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(MULTIPLE_ACCEPTED_DEALS,
                                "物品 #" + rs.getLong("item_id") + " 同时有 "
                                        + rs.getInt("total") + " 条已接受流转",
                                rs.getLong("item_id"));
                    }
                });
    }

    /** 已完成状态缺少成交时间，或未完成状态却写了成交时间。 */
    List<ConsistencyIssue> findCompletedAtMismatch() {
        return collect(
                "SELECT id, status, completed_at FROM deals WHERE"
                        + " (status = 'COMPLETED' AND (completed_at IS NULL OR TRIM(completed_at) = ''))"
                        + " OR (status <> 'COMPLETED' AND completed_at IS NOT NULL"
                        + "     AND TRIM(completed_at) <> '')",
                COMPLETED_AT_MISMATCH,
                new RowReader() {
                    @Override
                    public ConsistencyIssue read(ResultSet rs) throws SQLException {
                        return new ConsistencyIssue(COMPLETED_AT_MISMATCH,
                                "流转 #" + rs.getLong("id") + " 状态为 " + rs.getString("status")
                                        + "，成交时间与状态不匹配",
                                rs.getLong("id"));
                    }
                });
    }

    private List<ConsistencyIssue> collect(String sql, String rule, RowReader reader) {
        List<ConsistencyIssue> issues = new ArrayList<>();
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                issues.add(reader.read(resultSet));
            }
            return issues;
        } catch (SQLException e) {
            throw new DataAccessException("数据一致性检查失败，请稍后重试",
                    "一致性检查：" + rule + "：" + sql, e);
        }
    }

    private interface RowReader {
        ConsistencyIssue read(ResultSet resultSet) throws SQLException;
    }
}
