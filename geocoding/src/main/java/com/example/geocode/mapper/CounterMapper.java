package com.example.geocode.mapper;

import com.example.geocode.entity.CounterResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

@Mapper
public interface CounterMapper {

    @Select("CALL decrease_counter_monthly(#{decreaseAmount})")
    @Options(statementType = StatementType.CALLABLE)
    CounterResult decreaseCounter(@Param("decreaseAmount") int decreaseAmount);

    @Select("CALL decrease_counter_daily(#{decreaseAmount})")
    @Options(statementType = StatementType.CALLABLE)
    CounterResult decreaseDailyCounter(@Param("decreaseAmount") int decreaseAmount);
}