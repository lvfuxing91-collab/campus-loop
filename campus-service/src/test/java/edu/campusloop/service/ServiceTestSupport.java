package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.enums.Role;
import edu.campusloop.core.enums.TradeType;
import edu.campusloop.core.model.CarbonFactor;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.Review;
import edu.campusloop.core.model.User;
import edu.campusloop.core.query.ItemQuery;
import edu.campusloop.core.repository.CarbonFactorRepository;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.ReviewRepository;
import edu.campusloop.core.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ServiceTestSupport {

    static final TransactionRunner DIRECT_TRANSACTION = work -> work.run();

    private ServiceTestSupport() {
    }

    static final class Users implements UserRepository {

        private final Map<Long, User> store = new LinkedHashMap<>();
        private long sequence = 1L;

        User add(String username, Role role, boolean enabled) {
            User user = new User();
            user.setId(sequence++);
            user.setUsername(username);
            user.setNickname(username);
            user.setRole(role);
            user.setEnabled(enabled);
            user.setCreditScore(100.0);
            store.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            for (User user : store.values()) {
                if (user.getUsername().equals(username)) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public int count() {
            return store.size();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public long insert(User user) {
            user.setId(sequence++);
            store.put(user.getId(), user);
            return user.getId();
        }

        @Override
        public void update(User user) {
            store.put(user.getId(), user);
        }
    }

    static final class Items implements ItemRepository {

        private final Map<Long, Item> store = new LinkedHashMap<>();
        private long sequence = 1L;

        Item add(long ownerId, String categoryName, String title, ItemStatus status, TradeType type) {
            Item item = new Item();
            item.setId(sequence++);
            item.setOwnerId(ownerId);
            item.setCategoryId(1L);
            item.setCategoryName(categoryName);
            item.setTitle(title);
            item.setDescription("测试物品");
            item.setStatus(status);
            item.setTradeType(type);
            item.setPrice(new BigDecimal("10"));
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            store.put(item.getId(), item);
            return copy(item);
        }

        @Override
        public long insert(Item item) {
            item.setId(sequence++);
            store.put(item.getId(), copy(item));
            return item.getId();
        }

        @Override
        public void update(Item item) {
            store.put(item.getId(), copy(item));
        }

        @Override
        public Optional<Item> findById(long id) {
            return Optional.ofNullable(store.get(id)).map(Items::copy);
        }

        @Override
        public List<Item> search(ItemQuery query) {
            return new ArrayList<>();
        }

        @Override
        public List<Item> findByOwner(long ownerId) {
            List<Item> result = new ArrayList<>();
            for (Item item : store.values()) {
                if (item.getOwnerId() == ownerId) {
                    result.add(copy(item));
                }
            }
            return result;
        }

        @Override
        public int countAll() {
            return store.size();
        }

        @Override
        public int countByStatus(ItemStatus status) {
            int count = 0;
            for (Item item : store.values()) {
                if (item.getStatus() == status) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Map<String, Integer> countByCategory() {
            Map<String, Integer> result = new LinkedHashMap<>();
            for (Item item : store.values()) {
                String name = item.getCategoryName() == null ? "未分类" : item.getCategoryName();
                Integer current = result.get(name);
                result.put(name, current == null ? 1 : current + 1);
            }
            return result;
        }

        @Override
        public void incrementViewCount(long itemId) {
            Item item = store.get(itemId);
            if (item != null) {
                item.setViewCount(item.getViewCount() + 1);
            }
        }

        @Override
        public void updateStatus(long itemId, ItemStatus status) {
            Item item = store.get(itemId);
            if (item != null) {
                item.setStatus(status);
            }
        }

        private static Item copy(Item source) {
            Item copy = new Item();
            copy.setId(source.getId());
            copy.setOwnerId(source.getOwnerId());
            copy.setOwnerNickname(source.getOwnerNickname());
            copy.setCategoryId(source.getCategoryId());
            copy.setCategoryName(source.getCategoryName());
            copy.setTitle(source.getTitle());
            copy.setDescription(source.getDescription());
            copy.setCondition(source.getCondition());
            copy.setTradeType(source.getTradeType());
            copy.setPrice(source.getPrice());
            copy.setStatus(source.getStatus());
            copy.setTags(source.getTags());
            copy.setImagePath(source.getImagePath());
            copy.setViewCount(source.getViewCount());
            copy.setCreatedAt(source.getCreatedAt());
            copy.setUpdatedAt(source.getUpdatedAt());
            return copy;
        }
    }

    static final class Deals implements DealRepository {

        private final Map<Long, Deal> store = new LinkedHashMap<>();
        private final Map<String, Integer> completedByCategory = new LinkedHashMap<>();
        private long sequence = 1L;

        Deal add(long itemId, long buyerId, long sellerId, DealStatus status) {
            Deal deal = new Deal();
            deal.setId(sequence++);
            deal.setItemId(itemId);
            deal.setBuyerId(buyerId);
            deal.setSellerId(sellerId);
            deal.setType(TradeType.SELL);
            deal.setStatus(status);
            deal.setCreatedAt(LocalDateTime.now());
            deal.setUpdatedAt(LocalDateTime.now());
            store.put(deal.getId(), deal);
            return copy(deal);
        }

        void setCompletedByCategory(Map<String, Integer> values) {
            completedByCategory.clear();
            completedByCategory.putAll(values);
        }

        @Override
        public long insert(Deal deal) {
            deal.setId(sequence++);
            store.put(deal.getId(), copy(deal));
            return deal.getId();
        }

        @Override
        public void update(Deal deal) {
            store.put(deal.getId(), copy(deal));
        }

        @Override
        public Optional<Deal> findById(long id) {
            return Optional.ofNullable(store.get(id)).map(Deals::copy);
        }

        @Override
        public List<Deal> findByUser(long userId) {
            List<Deal> result = new ArrayList<>();
            for (Deal deal : store.values()) {
                if (deal.getBuyerId() == userId || deal.getSellerId() == userId) {
                    result.add(copy(deal));
                }
            }
            return result;
        }

        @Override
        public List<Deal> findByItem(long itemId) {
            List<Deal> result = new ArrayList<>();
            for (Deal deal : store.values()) {
                if (deal.getItemId() == itemId) {
                    result.add(copy(deal));
                }
            }
            return result;
        }

        @Override
        public int countAll() {
            return store.size();
        }

        @Override
        public int countByStatus(DealStatus status) {
            int count = 0;
            for (Deal deal : store.values()) {
                if (deal.getStatus() == status) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public Map<String, Integer> countCompletedByCategory() {
            return new LinkedHashMap<>(completedByCategory);
        }

        private static Deal copy(Deal source) {
            Deal copy = new Deal();
            copy.setId(source.getId());
            copy.setItemId(source.getItemId());
            copy.setItemTitle(source.getItemTitle());
            copy.setBuyerId(source.getBuyerId());
            copy.setBuyerNickname(source.getBuyerNickname());
            copy.setSellerId(source.getSellerId());
            copy.setSellerNickname(source.getSellerNickname());
            copy.setType(source.getType());
            copy.setMessage(source.getMessage());
            copy.setStatus(source.getStatus());
            copy.setCreatedAt(source.getCreatedAt());
            copy.setUpdatedAt(source.getUpdatedAt());
            copy.setCompletedAt(source.getCompletedAt());
            return copy;
        }
    }

    static final class Reviews implements ReviewRepository {

        private final List<Review> store = new ArrayList<>();
        private long sequence = 1L;

        @Override
        public long insert(Review review) {
            review.setId(sequence++);
            store.add(copy(review));
            return review.getId();
        }

        @Override
        public List<Review> findByReviewee(long revieweeId) {
            List<Review> result = new ArrayList<>();
            for (Review review : store) {
                if (review.getRevieweeId() == revieweeId) {
                    result.add(copy(review));
                }
            }
            return result;
        }

        @Override
        public List<Review> findByDeal(long dealId) {
            List<Review> result = new ArrayList<>();
            for (Review review : store) {
                if (review.getDealId() == dealId) {
                    result.add(copy(review));
                }
            }
            return result;
        }

        @Override
        public boolean existsByDealAndReviewer(long dealId, long reviewerId) {
            for (Review review : store) {
                if (review.getDealId() == dealId && review.getReviewerId() == reviewerId) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public double averageRating(long revieweeId) {
            double total = 0.0;
            int count = 0;
            for (Review review : store) {
                if (review.getRevieweeId() == revieweeId) {
                    total += review.getRating();
                    count++;
                }
            }
            return count == 0 ? 0.0 : total / count;
        }

        private static Review copy(Review source) {
            Review copy = new Review();
            copy.setId(source.getId());
            copy.setDealId(source.getDealId());
            copy.setReviewerId(source.getReviewerId());
            copy.setReviewerNickname(source.getReviewerNickname());
            copy.setRevieweeId(source.getRevieweeId());
            copy.setRating(source.getRating());
            copy.setComment(source.getComment());
            copy.setCreatedAt(source.getCreatedAt());
            return copy;
        }
    }

    static final class Logs implements OperationLogRepository {

        private final List<OperationLog> entries = new ArrayList<>();

        @Override
        public void insert(OperationLog log) {
            entries.add(log);
        }

        @Override
        public List<OperationLog> findRecent(int limit) {
            return new ArrayList<>(entries);
        }

        OperationLog last() {
            return entries.isEmpty() ? null : entries.get(entries.size() - 1);
        }
    }

    static final class CarbonFactors implements CarbonFactorRepository {

        private final Map<Long, CarbonFactor> store = new LinkedHashMap<>();
        private long sequence = 1L;

        CarbonFactor add(String categoryName, double value) {
            CarbonFactor factor = new CarbonFactor();
            factor.setCategoryId(sequence++);
            factor.setCategoryName(categoryName);
            factor.setKgCo2SavedPerItem(value);
            store.put(factor.getCategoryId(), factor);
            return factor;
        }

        @Override
        public List<CarbonFactor> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<CarbonFactor> findByCategoryId(long categoryId) {
            return Optional.ofNullable(store.get(categoryId));
        }
    }
}
