package edu.campusloop.dao.repository;

import edu.campusloop.core.enums.ItemCondition;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.util.DateTimes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ItemMapper {

    static final String SELECT =
            "SELECT i.id, i.owner_id, u.nickname AS owner_nickname, i.category_id,"
                    + " c.name AS category_name, i.title, i.description, i.condition_level,"
                    + " i.trade_type, i.price, i.status, i.tags, i.image_path, i.view_count,"
                    + " i.created_at, i.updated_at"
                    + " FROM items i"
                    + " JOIN users u ON u.id = i.owner_id"
                    + " JOIN categories c ON c.id = i.category_id";

    private ItemMapper() {
    }

    static Item map(ResultSet resultSet) throws SQLException {
        Item item = new Item();
        item.setId(resultSet.getLong("id"));
        item.setOwnerId(resultSet.getLong("owner_id"));
        item.setOwnerNickname(resultSet.getString("owner_nickname"));
        item.setCategoryId(resultSet.getLong("category_id"));
        item.setCategoryName(resultSet.getString("category_name"));
        item.setTitle(resultSet.getString("title"));
        item.setDescription(resultSet.getString("description"));
        item.setCondition(ItemCondition.valueOf(resultSet.getString("condition_level")));
        item.setTradeType(TradeType.valueOf(resultSet.getString("trade_type")));
        item.setPrice(java.math.BigDecimal.valueOf(resultSet.getDouble("price")));
        item.setStatus(ItemStatus.valueOf(resultSet.getString("status")));
        item.setTags(parseTags(resultSet.getString("tags")));
        item.setImagePath(resultSet.getString("image_path"));
        item.setViewCount(resultSet.getInt("view_count"));
        item.setCreatedAt(DateTimes.parse(resultSet.getString("created_at")));
        item.setUpdatedAt(DateTimes.parse(resultSet.getString("updated_at")));
        return item;
    }

    static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String tag : tags) {
            String trimmed = tag == null ? "" : tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(trimmed);
        }
        return builder.toString();
    }

    private static List<String> parseTags(String raw) {
        List<String> tags = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return tags;
        }
        tags.addAll(Arrays.asList(raw.split(",")));
        return tags;
    }
}
