package com.kms.springbatchprj.batchConfig;

import com.kms.springbatchprj.entity.CustomerEntity;
import org.springframework.batch.item.ItemProcessor;

public class CustomerProcessor implements ItemProcessor<CustomerEntity, CustomerEntity> {
    @Override
    public CustomerEntity process(CustomerEntity customerEntity) throws Exception {
        return customerEntity;
    }
}
