package com.campus.runner.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.runner.common.ErrandOrderPageQuery;
import com.campus.runner.entity.ErrandOrder;

public interface ErrandOrderService extends IService<ErrandOrder> {

    /**
     * 骑手接单：校验状态机并锁定订单
     *
     * @param orderId 订单 ID
     * @param runnerId 接单骑手 ID
     */
    void acceptOrder(Long orderId, Long runnerId);

    /**
     * 多条件组合分页查询
     *
     * @param query 查询参数
     * @return 分页结果
     */
    IPage<ErrandOrder> pageQuery(ErrandOrderPageQuery query);

    // ==================== 状态流转接口 ====================

    /**
     * 骑手确认送达：0→2
     *
     * @param orderId 订单 ID
     * @param runnerId 接单骑手 ID（必须与当前登录用户一致）
     */
    void confirmDelivery(Long orderId, Long runnerId);

    /**
     * 发单同学确认收货（完成订单）：2→3
     *
     * @param orderId 订单 ID
     * @param clientId 发单人 ID（必须与当前登录用户一致）
     */
    void completeOrder(Long orderId, Long clientId);

    /**
     * 发单同学取消订单：逻辑删除（仅 status=0 可取消）
     *
     * @param orderId 订单 ID
     * @param clientId 发单人 ID
     */
    void cancelOrder(Long orderId, Long clientId);

    // ==================== 个人订单查询 ====================

    /**
     * 查询当前用户发布的订单（分页）
     *
     * @param query 分页参数
     * @param clientId 当前登录用户 ID
     * @return 分页结果
     */
    IPage<ErrandOrder> myPublished(ErrandOrderPageQuery query, Long clientId);

    /**
     * 查询当前用户抢到的订单（分页）
     *
     * @param query 分页参数
     * @param runnerId 当前登录用户 ID
     * @return 分页结果
     */
    IPage<ErrandOrder> myAccepted(ErrandOrderPageQuery query, Long runnerId);
}