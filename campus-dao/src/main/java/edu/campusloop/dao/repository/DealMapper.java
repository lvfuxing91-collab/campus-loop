package edu.campusloop.dao.repository;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.util.DateTimes;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 流转记录映射。
 * <p>
 * 物品名称与买卖双方昵称是展示字段，通过联结 items 与两次 users 取得，
 * 不重复保存到 deals 表。
 */
final class DealMapper {

    static final String SELECT =
            "SELECT d.id, d.item_id, i.title AS item_title, d.buyer_id,"
                    + " bu.nickname AS buyer_nickname, d.seller_id, su.nickname AS seller_nickname,"
                    + " d.type, d.message, d.status, d.created_at, d.updated_at, d.completed_at"
                    + " FROM deals d"
                    + " JOIN items i ON i.id = d.item_id"
                    + " JOIN users bu ON bu.id = d.buyer_id"
                    + " JOIN users su ON su.id = d.seller_id";

    private DealMapper() {
    }

    static Deal map(ResultSet resultSet) throws SQLException {
        Deal deal = new Deal();
        deal.setId(resultSet.getLong("id"));
        deal.setItemId(resultSet.getLong("item_id"));
        deal.setItemTitle(resultSet.getString("item_title"));
        deal.setBuyerId(resultSet.getLong("buyer_id"));
        deal.setBuyerNickname(resultSet.getString("buyer_nickname"));
        deal.setSellerId(resultSet.getLong("seller_id"));
        deal.setSellerNickname(resultSet.getString("seller_nickname"));
        deal.setType(TradeType.valueOf(resultSet.getString("type")));
        deal.setMessage(resultSet.getString("message"));
        deal.setStatus(DealStatus.valueOf(resultSet.getString("status")));
        deal.setCreatedAt(DateTimes.parse(resultSet.getString("created_at")));
        deal.setUpdatedAt(DateTimes.parse(resultSet.getString("updated_at")));
        deal.setCompletedAt(DateTimes.parse(resultSet.getString("completed_at")));
        return deal;
    }
}
