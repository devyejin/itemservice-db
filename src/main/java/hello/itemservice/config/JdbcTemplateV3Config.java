package hello.itemservice.config;

import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.jdbctemplate.JdbcTemplateItemRepositoryV3;
import hello.itemservice.service.ItemService;
import hello.itemservice.service.ItemServiceV1;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor //JdbcTemplateV1Config 객체가 생성될 때 필드 주 주입이됨
public class JdbcTemplateV3Config {

    private final DataSource dataSource; //지금 여기 주입되는 구현체는 누군데?

    @Bean
    public ItemService itemService() {

        return new ItemServiceV1(itemRepository());
    }

    @Bean
    public ItemRepository itemRepository() {

        return new JdbcTemplateItemRepositoryV3(dataSource);
    }
}
