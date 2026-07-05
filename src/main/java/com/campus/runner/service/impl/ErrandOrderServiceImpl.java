package com.campus.runner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.runner.common.BusinessException;
import com.campus.runner.common.ErrorCode;
import com.campus.runner.common.ErrandOrderPageQuery;
import com.campus.runner.entity.ErrandOrder;
import com.campus.runner.mapper.ErrandOrderMapper;
import com.campus.runner.service.ErrandOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ErrandOrderServiceImpl extends ServiceImpl<ErrandOrderMapper, ErrandOrder> implements ErrandOrderService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptOrder(Long orderId, Long runnerId) {
        // 1. 校验订单是否存在
        ErrandOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 2. 校验状态是否为 0（待接单）
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前订单状态为 " + order.getStatus() + "，仅待接单(0)状态可接单");
        }

        // 3. 锁定订单：更新状态为 1（配送中），注入接单骑手 ID
        order.setStatus(1);
        order.setRunnerId(runnerId);
        this.updateById(order);
    }

    // ==================== 状态流转 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDelivery(Long orderId, Long runnerId) {
        ErrandOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!runnerId.equals(order.getRunnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅接单骑手可确认送达");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前订单状态为 " + order.getStatus() + "，仅配送中(1)状态可确认送达");
        }
        order.setStatus(2); // 已送达
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId, Long clientId) {
        ErrandOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!clientId.equals(order.getClientId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发单同学可确认收货");
        }
        if (order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前订单状态为 " + order.getStatus() + "，仅已送达(2)状态可确认收货");
        }
        order.setStatus(3); // 已完成
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long clientId) {
        ErrandOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!clientId.equals(order.getClientId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发单同学可取消订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "当前订单状态为 " + order.getStatus() + "，仅待接单(0)状态可取消");
        }
        this.removeById(orderId); // 逻辑删除
    }

    // ==================== 个人订单查询 ====================

    @Override
    public IPage<ErrandOrder> myPublished(ErrandOrderPageQuery query, Long clientId) {
        Page<ErrandOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getClientId, clientId)
                .like(StringUtils.hasText(query.getTitle()), ErrandOrder::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getPickupAddress()), ErrandOrder::getPickupAddress, query.getPickupAddress())
                .like(StringUtils.hasText(query.getShippingAddress()), ErrandOrder::getShippingAddress, query.getShippingAddress())
                .eq(query.getStatus() != null, ErrandOrder::getStatus, query.getStatus())
                .orderByDesc(ErrandOrder::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public IPage<ErrandOrder> myAccepted(ErrandOrderPageQuery query, Long runnerId) {
        Page<ErrandOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRunnerId, runnerId)
                .like(StringUtils.hasText(query.getTitle()), ErrandOrder::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getPickupAddress()), ErrandOrder::getPickupAddress, query.getPickupAddress())
                .like(StringUtils.hasText(query.getShippingAddress()), ErrandOrder::getShippingAddress, query.getShippingAddress())
                .eq(query.getStatus() != null, ErrandOrder::getStatus, query.getStatus())
                .orderByDesc(ErrandOrder::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public IPage<ErrandOrder> pageQuery(ErrandOrderPageQuery query) {
        // 构建分页对象
        Page<ErrandOrder> page = new Page<>(query.getPageNum(), query.getPageSize());

        // 动态拼接查询条件
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<ErrandOrder>()
                .ge(query.getErrandFeeMin() != null, ErrandOrder::getErrandFee, query.getErrandFeeMin())
                .le(query.getErrandFeeMax() != null, ErrandOrder::getErrandFee, query.getErrandFeeMax())
                .like(StringUtils.hasText(query.getTitle()), ErrandOrder::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getPickupAddress()), ErrandOrder::getPickupAddress, query.getPickupAddress())
                .like(StringUtils.hasText(query.getShippingAddress()), ErrandOrder::getShippingAddress, query.getShippingAddress())
                .eq(query.getStatus() != null, ErrandOrder::getStatus, query.getStatus())
                .orderByDesc(ErrandOrder::getCreateTime);

        return this.page(page, wrapper);
    }
}