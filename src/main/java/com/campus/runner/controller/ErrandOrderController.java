package com.campus.runner.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.runner.common.BusinessException;
import com.campus.runner.common.ErrorCode;
import com.campus.runner.common.ErrandOrderDTO;
import com.campus.runner.common.ErrandOrderPageQuery;
import com.campus.runner.common.Result;
import com.campus.runner.common.SecurityUtil;
import com.campus.runner.entity.ErrandOrder;
import com.campus.runner.entity.User;
import com.campus.runner.service.ErrandOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "跑腿订单", description = "跑腿订单的增删改查与状态流转")
@RestController
@RequestMapping("/errand/order")
public class ErrandOrderController {

    @Autowired
    private ErrandOrderService errandOrderService;

    @Autowired
    private SecurityUtil securityUtil;

    @Operation(summary = "发布跑腿订单")
    @PostMapping
    public Result<ErrandOrder> create(@Valid @RequestBody ErrandOrderDTO dto, HttpServletRequest request) {
        // 从登录 Token 解析发单人 ID，避免硬编码 clientId=1
        User currentUser = requireLogin(request);
        ErrandOrder order = new ErrandOrder();
        order.setClientId(currentUser.getId());
        order.setTitle(dto.getTitle());
        order.setDescription(dto.getDescription());
        order.setErrandFee(dto.getErrandFee());
        order.setGoodsPrice(dto.getGoodsPrice());
        order.setPickupAddress(dto.getPickupAddress());
        order.setShippingAddress(dto.getShippingAddress());
        order.setStatus(0);

        errandOrderService.save(order);
        return Result.ok(order);
    }

    @Operation(summary = "根据 ID 查询跑腿订单")
    @GetMapping("/{id}")
    public Result<ErrandOrder> getById(@PathVariable Long id) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return Result.ok(order);
    }

    @Operation(summary = "查询全部跑腿订单")
    @GetMapping
    public Result<List<ErrandOrder>> list() {
        List<ErrandOrder> list = errandOrderService.list();
        return Result.ok(list);
    }

    @Operation(summary = "多条件组合分页查询")
    @GetMapping("/page")
    public Result<IPage<ErrandOrder>> page(ErrandOrderPageQuery query) {
        IPage<ErrandOrder> page = errandOrderService.pageQuery(query);
        return Result.ok(page);
    }

    @Operation(summary = "骑手接单", description = "校验状态机(0→1)，锁定订单并注入骑手 ID")
    @PutMapping("/{id}/accept")
    public Result<Void> acceptOrder(
            @Parameter(description = "订单 ID") @PathVariable Long id,
            @Parameter(description = "接单骑手 ID") @RequestParam Long runnerId,
            HttpServletRequest request) {
        // 越权校验：runnerId 必须等于当前登录用户
        User currentUser = requireLogin(request);
        if (!runnerId.equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能抢单给自己");
        }
        errandOrderService.acceptOrder(id, runnerId);
        return Result.ok();
    }

    @Operation(summary = "更新订单状态")
    @PutMapping("/{id}/status")
    public Result<ErrandOrder> updateStatus(
            @Parameter(description = "订单 ID") @PathVariable Long id,
            @Parameter(description = "目标状态") @RequestParam Integer status) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        order.setStatus(status);
        errandOrderService.updateById(order);
        return Result.ok(order);
    }

    @Operation(summary = "删除跑腿订单（逻辑删除）", description = "管理员可删除任意订单；普通用户仅可删除自己发布的订单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        // 1. 必须登录
        User currentUser = requireLogin(request);

        // 2. 订单必须存在
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 3. 越权校验：管理员放行；否则必须是订单发单人本人
        if (!SecurityUtil.isAdmin(currentUser)
                && (order.getClientId() == null || !order.getClientId().equals(currentUser.getId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        errandOrderService.removeById(id);
        return Result.ok();
    }

    /**
     * 从请求头解析当前登录用户；未登录则抛 401 业务异常。
     */
    private User requireLogin(HttpServletRequest request) {
        User user = securityUtil.getCurrentUser(request.getHeader("Authorization"));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    // ==================== 状态流转接口 ====================

    @Operation(summary = "骑手确认送达", description = "状态机(1→2)，仅接单骑手可操作")
    @PutMapping("/{id}/deliver")
    public Result<Void> confirmDelivery(
            @Parameter(description = "订单 ID") @PathVariable Long id,
            HttpServletRequest request) {
        User currentUser = requireLogin(request);
        errandOrderService.confirmDelivery(id, currentUser.getId());
        return Result.ok();
    }

    @Operation(summary = "发单同学确认收货", description = "状态机(2→3)，仅发单人可操作")
    @PutMapping("/{id}/complete")
    public Result<ErrandOrder> completeOrder(
            @Parameter(description = "订单 ID") @PathVariable Long id,
            HttpServletRequest request) {
        User currentUser = requireLogin(request);
        errandOrderService.completeOrder(id, currentUser.getId());
        ErrandOrder order = errandOrderService.getById(id);
        return Result.ok(order);
    }

    @Operation(summary = "发单同学取消订单", description = "仅待接单(0)状态可取消，逻辑删除")
    @DeleteMapping("/{id}/cancel")
    public Result<Void> cancelOrder(
            @Parameter(description = "订单 ID") @PathVariable Long id,
            HttpServletRequest request) {
        User currentUser = requireLogin(request);
        errandOrderService.cancelOrder(id, currentUser.getId());
        return Result.ok();
    }

    // ==================== 个人订单查询 ====================

    @Operation(summary = "我的发布订单", description = "分页查询当前登录用户发布的订单")
    @GetMapping("/my-published")
    public Result<IPage<ErrandOrder>> myPublished(
            ErrandOrderPageQuery query,
            HttpServletRequest request) {
        User currentUser = requireLogin(request);
        IPage<ErrandOrder> page = errandOrderService.myPublished(query, currentUser.getId());
        return Result.ok(page);
    }

    @Operation(summary = "我的抢单订单", description = "分页查询当前登录用户抢到的订单")
    @GetMapping("/my-accepted")
    public Result<IPage<ErrandOrder>> myAccepted(
            ErrandOrderPageQuery query,
            HttpServletRequest request) {
        User currentUser = requireLogin(request);
        IPage<ErrandOrder> page = errandOrderService.myAccepted(query, currentUser.getId());
        return Result.ok(page);
    }
}