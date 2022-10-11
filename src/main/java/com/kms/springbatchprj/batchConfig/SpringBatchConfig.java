package com.kms.springbatchprj.batchConfig;

import com.kms.springbatchprj.entity.CustomerEntity;
import com.kms.springbatchprj.processorConfig.CustomerProcessor;
import com.kms.springbatchprj.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@AllArgsConstructor
public class SpringBatchConfig {
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private CustomerRepository customerRepository;
//    this is the reader, who response for the reading process from sources
    @Bean
    public FlatFileItemReader<CustomerEntity> reader() {
        FlatFileItemReader<CustomerEntity> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource("src/main/resources/customers.csv"));
        itemReader.setName("csvReader");
//        Skip header in csv file. E.g: full-name, lastname...
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    @Bean
    public FlatFileItemWriter<CustomerEntity> writerToCsv() {
        FlatFileItemWriter<CustomerEntity> itemWriter = new FlatFileItemWriter<>();
        itemWriter.setName("csvWriter");
        itemWriter.setResource(new FileSystemResource("src/main/resources/export.csv"));
        itemWriter.setLineAggregator(getDelimitedLineAggregator());
        return itemWriter;
    }

    public DelimitedLineAggregator<CustomerEntity> getDelimitedLineAggregator() {
        BeanWrapperFieldExtractor<CustomerEntity> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<CustomerEntity>();
        beanWrapperFieldExtractor.setNames(new String[] {"id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob"});
        DelimitedLineAggregator<CustomerEntity> delimitedLineAggregator = new DelimitedLineAggregator<CustomerEntity>();
        delimitedLineAggregator.setDelimiter(",");
        delimitedLineAggregator.setFieldExtractor(beanWrapperFieldExtractor);
        return delimitedLineAggregator;
    }

    //    mapper is just act like a supporter for reader to mapping...
    private LineMapper<CustomerEntity> lineMapper() {
        DefaultLineMapper<CustomerEntity> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();

        delimitedLineTokenizer.setDelimiter(",");
        delimitedLineTokenizer.setStrict(false);
        delimitedLineTokenizer.setNames("id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob");

        BeanWrapperFieldSetMapper<CustomerEntity> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(CustomerEntity.class);

        lineMapper.setLineTokenizer(delimitedLineTokenizer);

        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
    }
//    this is where we integrated the item processor in
    @Bean
    public CustomerProcessor customerProcessor() {
        return new CustomerProcessor();
    }
//    this writer means it will use the customer repository with method save to save data read from reader
    @Bean
    public RepositoryItemWriter<CustomerEntity> writer(){
        RepositoryItemWriter<CustomerEntity> itemWriter = new RepositoryItemWriter<>();
        itemWriter.setRepository(customerRepository);
        itemWriter.setMethodName("save");
        return itemWriter;
    }

    @Bean
    public RepositoryItemReader<CustomerEntity> readerFromDatabase() {
        RepositoryItemReader<CustomerEntity> itemReader = new RepositoryItemReader<>();
        itemReader.setRepository(customerRepository);
        itemReader.setMethodName("findAll");
        Map<String, Sort.Direction> map = new HashMap<>();
        map.put("id", Sort.Direction.ASC);
        itemReader.setSort(map);
        return itemReader;
    }

//    chunk = 10 means this step will work on 10 lines of csv file / time - amount of data
    @Bean
    public Step writeCsvToDatabaseStep () {
        return stepBuilderFactory.get("csv-database").<CustomerEntity, CustomerEntity>
                chunk(10)
                .reader(reader())
                .processor(customerProcessor())
                .writer(writer())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step exportDataToCsvStep() {
        return stepBuilderFactory.get("database-csv").<CustomerEntity, CustomerEntity>
                chunk(10)
                .reader(readerFromDatabase())
                .processor(customerProcessor())
                .writer(writerToCsv())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Job exportDbToCsvJob() {
        return jobBuilderFactory.get("export-customer")
                .flow(exportDataToCsvStep())
                .end().build();
    }

    /*@Bean
    public Job writeCsvToDatabaseJob() {
        return jobBuilderFactory.get("import-customer")
                .flow(writeCsvToDatabaseStep())
                .end().build();
    }*/

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setConcurrencyLimit(10);
        return asyncTaskExecutor;
    }

//    pattern mình đang theo là : job -> nhiều step -> mỗi step co reader, processor,writer ( mỗi bean khác nhau)
//    tất cả các task chia ra theo 1 step. Sau đó gom task về 1 step -> assign step đó vào job

//    pattern của tasklet là 1 step có 1 task duy nhất. Ví dụ step reader chỉ có task reader.
//    sau đó gom hết về step ve 1 job

}
