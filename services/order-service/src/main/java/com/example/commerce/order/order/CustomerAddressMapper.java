package com.example.commerce.order.order;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomerAddressMapper {

    List<CustomerAddress> findByCustomerId(String customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(
            @Param("id") String id, @Param("customerId") String customerId);

    Optional<CustomerAddress> findDefaultByCustomerId(String customerId);

    int clearDefault(String customerId);

    int insert(CustomerAddress address);

    int update(CustomerAddress address);

    int deleteByIdAndCustomerId(
            @Param("id") String id, @Param("customerId") String customerId);
}
