package com.campus.runner.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.runner.entity.OrderRate;
import com.campus.runner.mapper.OrderRateMapper;
import com.campus.runner.service.OrderRateService;
import org.springframework.stereotype.Service;

@Service
public class OrderRateServiceImpl extends ServiceImpl<OrderRateMapper, OrderRate> implements OrderRateService {
}