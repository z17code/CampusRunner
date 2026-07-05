package com.campus.runner.controller;

import com.campus.runner.entity.ErrandOrder;
import com.campus.runner.service.ErrandOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ErrandOrderService errandOrderService;

    @PostMapping("/order")
    public String createTestOrder() {
        ErrandOrder order = new ErrandOrder();
        order.setClientId(1L);
        order.setTitle("测试跑腿单");
        order.setDescription("帮我去食堂买一份黄焖鸡米饭");
        order.setErrandFee(new BigDecimal("3.00"));
        order.setGoodsPrice(new BigDecimal("18.00"));
        order.setPickupAddress("二食堂二楼黄焖鸡窗口");
        order.setShippingAddress("11栋宿舍楼303室");
        order.setStatus(0);

        boolean saved = errandOrderService.save(order);
        return saved ? "插入成功，订单ID = " + order.getId() : "插入失败";
    }

    @GetMapping("/order")
    public String listTestOrders() {
        List<ErrandOrder> list = errandOrderService.list();
        if (list.isEmpty()) {
            return "暂无跑腿订单记录";
        }
        StringBuilder sb = new StringBuilder("共查询到 ").append(list.size()).append(" 条订单记录：<br>");
        for (ErrandOrder o : list) {
            sb.append("ID=").append(o.getId())
                    .append("，标题=").append(o.getTitle())
                    .append("，状态=").append(o.getStatus())
                    .append("，创建时间=").append(o.getCreateTime())
                    .append("<br>");
        }
        return sb.toString();
    }
}