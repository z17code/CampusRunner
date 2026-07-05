package com.campus.runner.controller;

import com.campus.runner.common.BusinessException;
import com.campus.runner.common.ErrorCode;
import com.campus.runner.common.OrderRateDTO;
import com.campus.runner.common.Result;
import com.campus.runner.common.SecurityUtil;
import com.campus.runner.entity.ErrandOrder;
import com.campus.runner.entity.OrderRate;
import com.campus.runner.entity.User;
import com.campus.runner.mapper.ErrandOrderMapper;
import com.campus.runner.service.OrderRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "服务评价", description = "订单完成后的星级评分与评价")
@RestController
@RequestMapping("/order-rate")
public class OrderRateController {

    @Autowired
    private OrderRateService orderRateService;

    @Autowired
    private ErrandOrderMapper errandOrderMapper;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 提交评价（仅已完成订单的发单人可评价，每个订单限评一次）
     */
    @Operation(summary = "提交订单评价", description = "发单同学对已完成的订单进行 1-5 星评分和文字评价")
    @PostMapping("/{orderId}")
    public Result<Void> submitRate(
            @Parameter(description = "订单 ID") @PathVariable Long orderId,
            @Valid @RequestBody OrderRateDTO dto,
            HttpServletRequest request) {
        // 校验登录
        var currentUser = securityUtil.getCurrentUser(request.getHeader("Authorization"));
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 查订单
        ErrandOrder order = errandOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 必须是发单人
        if (!currentUser.getId().equals(order.getClientId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅发单同学可评价");
        }
        // 必须已完成
        if (order.getStatus() != 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅已完成(3)状态的订单可评价");
        }
        // 防重复评价
        long existCount = orderRateService.lambdaQuery()
                .eq(OrderRate::getOrderId, orderId)
                .count();
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该订单已评价，不可重复提交");
        }

        OrderRate rate = new OrderRate();
        rate.setOrderId(orderId);
        rate.setClientId(currentUser.getId());
        rate.setRunnerId(order.getRunnerId());
        rate.setScore(dto.getScore());
        rate.setContent(dto.getContent());

        orderRateService.save(rate);
        return Result.ok();
    }
}
