package com.example.geocode.service.impl;

import com.example.geocode.mapper.CounterMapper;
import com.example.geocode.entity.CounterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuotaService {

    private final CounterMapper counterMapper;
    
    @Autowired
    public QuotaService(CounterMapper counterMapper) {
        this.counterMapper = counterMapper;
    }

    // 调用计数器服务并返回 150000 - 当前计数值
    // @param decreaseAmount 本次需要减少的数量
    // @return 计算结果对象
    public CounterResult useCounter(int decreaseAmount) {
        // 调用计数器服务减少计数值
        CounterResult result = counterMapper.decreaseCounter(decreaseAmount);
        // 返回包含计算结果的对象
        return result;
    }

    // 另一种形式：直接返回计算结果值（150000 - currentValue）
    // @param decreaseAmount 本次需要减少的数量
    // @return 计算结果值
    public int getCalculatedResult(int decreaseAmount) {
        CounterResult result = counterMapper.decreaseCounter(decreaseAmount);
        return result.getResultValue();
    }

    public int getCurrentValue() {
        CounterResult result = counterMapper.decreaseCounter(0);
        return result.getNewValue();
    }

//
//  daily
//
    public CounterResult useCounterDaily(int decreaseAmount) {
        // 调用计数器服务减少计数值
        CounterResult result = counterMapper.decreaseDailyCounter(decreaseAmount);
        // 返回包含计算结果的对象
        return result;
    }
    public int getCalculatedResultDaily(int decreaseAmount) {
        CounterResult result = counterMapper.decreaseDailyCounter(decreaseAmount);
        return result.getResultValue();
    }
    public int getCurrentValueDaily() {
        CounterResult result = counterMapper.decreaseDailyCounter(0);
        return result.getNewValue();
    }
}