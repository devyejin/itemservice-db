package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NamedParameterJdbcTemplate
 * SqlParameterSource
 *  - BeanPropertySqlParameterSource
 *  - MapSqlParameterSource
 * Map
 *
 * BeanPropertyRowMapper
 */

@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;


    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        //JdbcTemplate을 사용하려면, 커넥션 등 이런게 필요하잖아. 이걸 dataSource(커넥션풀을 얻는 방법을 추상화한 인터페이스)에서 얻어옴
        this.template = new NamedParameterJdbcTemplate(dataSource);

    }

    @Override
    public Item save(Item item) {




        String sql = "insert into item (item_name, price, quantity) " +
                "values (:itemName,:price, :quantity )";

        //ID값은 DB에서 생성하는
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 저장하려고 넘긴 Item 객체를 이용해서 param을 만드는 방식
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        template.update(sql,param, keyHolder);

        //쿼리 수행했으니까, db에서 키 값 받아올 수 있음
        long key = keyHolder.getKey().longValue();
        item.setId(key); // item에 db에서 생성한 id값 저장 후 반환
        return item;


    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set " +
                "item_name=:itemName, price=:price, quantity=:quantity " +
                "where id=:id";

        SqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                .addValue("id", itemId);

        template.update(sql, param);

    }

    @Override

    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name , price, quantity from item " +
                "where id = :id";

        try {

            Map<String, Object> param = Map.of("id", id);
            Item item = template.queryForObject(sql, param, itemRowMapper());
            //sql, param 을 넘기면 반환된 ResultSet을 item Object로 반환(itemRowMapper() )
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }

    }



    @Override
    public List<Item> findAll(ItemSearchCond cond) {
        String itemName = cond.getItemName();
        Integer maxPrice = cond.getMaxPrice();

        SqlParameterSource param = new BeanPropertySqlParameterSource(cond);

        String sql = "select id, item_name, price, quantity from item";

        //동적 쿼리

        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;


        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%',:itemName,'%')";
            andFlag = true;

        }

        if (maxPrice != null) {

            if (andFlag) {
                sql += " and";

            }
            sql += " price <= :maxPrice";


        }
        log.info("sql={}", sql);

        return template.query(sql, param, itemRowMapper());

    }

    private RowMapper<Item> itemRowMapper() {

    return BeanPropertyRowMapper.newInstance(Item.class); //carmel 지원 (db 스네이크 -> 자바 카멜)


    }
}