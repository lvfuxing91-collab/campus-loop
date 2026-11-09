package edu.campusloop.service;

import edu.campusloop.core.enums.DealStatus;
import edu.campusloop.core.enums.ItemStatus;
import edu.campusloop.core.exception.BusinessRuleException;
import edu.campusloop.core.exception.ValidationException;
import edu.campusloop.core.logging.AppLogger;
import edu.campusloop.core.model.Deal;
import edu.campusloop.core.model.Item;
import edu.campusloop.core.model.OperationLog;
import edu.campusloop.core.model.User;
import edu.campusloop.core.repository.DealRepository;
import edu.campusloop.core.repository.ItemRepository;
import edu.campusloop.core.repository.OperationLogRepository;
import edu.campusloop.core.repository.UserRepository;
import edu.campusloop.core.service.DealService;
import edu.campusloop.core.util.Validators;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

public class DealServiceImpl implements DealService {

    private static final Logger LOG = AppLogger.get(DealServiceImpl.class);
    private static final int MAX_MESSAGE_LENGTH = 300;

    private final UserRepository users;
    private final ItemRepository items;
    private final DealRepository deals;
    private final OperationLogRepository logs;
    private final TransactionRunner transactions;

    public DealServiceImpl(UserRepository users, ItemRepository items, DealRepository deals,
                           OperationLogRepository logs, TransactionRunner transactions) {
        this.users = users;
        this.items = items;
        this.deals = deals;
        this.logs = logs;
        this.transactions = transactions;
    }

    @Override
    public long createIntent(long itemId, long buyerId, String message) {
        Validators.requirePositiveId("物品", itemId);
        Validators.requirePositiveId("用户", buyerId);
        requireEnabledUser(buyerId);
        Item item = requireItem(itemId);
        if (item.getStatus() != ItemStatus.ON_SALE) {
            throw new BusinessRuleException("该物品当前不可发起流转");
        }
        if (item.isOwnedBy(buyerId)) {
            throw new BusinessRuleException("不能对自己的物品发起流转");
        }
        for (Deal existing : deals.findByItem(itemId)) {
            if (existing.getBuyerId() == buyerId
                    && (existing.getStatus() == DealStatus.PENDING
                    || existing.getStatus() == DealStatus.ACCEPTED)) {
                throw new BusinessRuleException("你已对该物品发起过流转意向");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Deal deal = new Deal();
        deal.setItemId(itemId);
        deal.setBuyerId(buyerId);
        deal.setSellerId(item.getOwnerId());
        deal.setType(item.getTradeType());
        deal.setMessage(optionalText("留言", message, MAX_MESSAGE_LENGTH));
        deal.setStatus(DealStatus.PENDING);
        deal.setCreatedAt(now);
        deal.setUpdatedAt(now);
        long id = deals.insert(deal);
        audit(buyerId, "发起流转", "对物品 #" + itemId + " 发起流转 #" + id);
        LOG.info("用户 " + buyerId + " 对物品 " + itemId + " 发起流转 " + id);
        return id;
    }

    @Override
    public void accept(long dealId, long operatorId) {
        transactions.inTransaction(() -> {
            Deal deal = requireDeal(dealId);
            requireEnabledUser(operatorId);
            requirePending(deal);
            requireSeller(deal, operatorId);
            Item item = requireItem(deal.getItemId());
            requireItemOwner(item, deal);
            if (item.getStatus() != ItemStatus.ON_SALE) {
                throw new BusinessRuleException("物品已被预订或完成，无法接受其他意向");
            }

            deal.setStatus(DealStatus.ACCEPTED);
            deal.setUpdatedAt(LocalDateTime.now());
            deals.update(deal);
            items.updateStatus(item.getId(), ItemStatus.RESERVED);
            audit(operatorId, "接受流转", "接受流转 #" + dealId);
        });
    }

    @Override
    public void reject(long dealId, long operatorId, String reason) {
        transactions.inTransaction(() -> {
            Deal deal = requireDeal(dealId);
            requireEnabledUser(operatorId);
            requirePending(deal);
            requireSeller(deal, operatorId);

            String reply = optionalText("拒绝原因", reason, MAX_MESSAGE_LENGTH);
            if (reply != null) {
                deal.setMessage(appendReply(deal.getMessage(), "卖家回复：" + reply));
            }
            deal.setStatus(DealStatus.REJECTED);
            deal.setUpdatedAt(LocalDateTime.now());
            deals.update(deal);
            audit(operatorId, "拒绝流转", "拒绝流转 #" + dealId);
        });
    }

    @Override
    public void complete(long dealId, long operatorId) {
        transactions.inTransaction(() -> {
            Deal deal = requireDeal(dealId);
            requireEnabledUser(operatorId);
            requireSeller(deal, operatorId);
            if (deal.getStatus() != DealStatus.ACCEPTED) {
                throw new BusinessRuleException("只有已接受的流转才能完成");
            }
            Item item = requireItem(deal.getItemId());
            requireItemOwner(item, deal);
            if (item.getStatus() != ItemStatus.RESERVED) {
                throw new BusinessRuleException("关联物品不是已预订状态，无法完成");
            }

            LocalDateTime now = LocalDateTime.now();
            deal.setStatus(DealStatus.COMPLETED);
            deal.setUpdatedAt(now);
            deal.setCompletedAt(now);
            deals.update(deal);
            items.updateStatus(item.getId(), ItemStatus.COMPLETED);
            audit(operatorId, "完成流转", "完成流转 #" + dealId);
        });
    }

    @Override
    public void cancel(long dealId, long operatorId) {
        transactions.inTransaction(() -> {
            Deal deal = requireDeal(dealId);
            requireEnabledUser(operatorId);
            if (!isParticipant(deal, operatorId)) {
                throw new BusinessRuleException("只有流转双方可以取消");
            }
            if (deal.getStatus() != DealStatus.PENDING && deal.getStatus() != DealStatus.ACCEPTED) {
                throw new BusinessRuleException("该流转已经结束，不能取消");
            }

            if (deal.getStatus() == DealStatus.ACCEPTED) {
                Item item = requireItem(deal.getItemId());
                if (item.getStatus() != ItemStatus.RESERVED) {
                    throw new BusinessRuleException("关联物品状态异常，无法取消");
                }
                items.updateStatus(item.getId(), ItemStatus.ON_SALE);
            }
            deal.setStatus(DealStatus.CANCELLED);
            deal.setUpdatedAt(LocalDateTime.now());
            deals.update(deal);
            audit(operatorId, "取消流转", "取消流转 #" + dealId);
        });
    }

    @Override
    public List<Deal> myDeals(long userId) {
        Validators.requirePositiveId("用户", userId);
        users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        return deals.findByUser(userId);
    }

    private User requireEnabledUser(long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("用户不存在"));
        if (!user.isEnabled()) {
            throw new BusinessRuleException("账号已停用，无法执行该操作");
        }
        return user;
    }

    private Item requireItem(long itemId) {
        return items.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("物品不存在"));
    }

    private Deal requireDeal(long dealId) {
        Validators.requirePositiveId("流转", dealId);
        return deals.findById(dealId)
                .orElseThrow(() -> new BusinessRuleException("流转记录不存在"));
    }

    private void requirePending(Deal deal) {
        if (deal.getStatus() != DealStatus.PENDING) {
            throw new BusinessRuleException("只有待确认的流转可以执行该操作");
        }
    }

    private void requireSeller(Deal deal, long operatorId) {
        if (deal.getSellerId() != operatorId) {
            throw new BusinessRuleException("只有卖家可以执行该操作");
        }
    }

    private void requireItemOwner(Item item, Deal deal) {
        if (item.getOwnerId() != deal.getSellerId()) {
            throw new BusinessRuleException("流转记录与物品归属不一致");
        }
    }

    private boolean isParticipant(Deal deal, long userId) {
        return deal.getBuyerId() == userId || deal.getSellerId() == userId;
    }

    private String optionalText(String fieldName, String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName, fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return text;
    }

    private String appendReply(String message, String reply) {
        return message == null || message.trim().isEmpty() ? reply : message + "\n" + reply;
    }

    private void audit(long userId, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        try {
            logs.insert(log);
        } catch (RuntimeException e) {
            LOG.warning("操作日志写入失败：" + e.getMessage());
        }
    }
}
