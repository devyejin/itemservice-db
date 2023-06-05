package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCond;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *  SimpleJdbcInsert
 */

@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV3 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;
    private final SimpleJdbcInsert jdbcInsert;


    public JdbcTemplateItemRepositoryV3(DataSource dataSource) {
        //JdbcTemplate을 사용하려면, 커넥션 등 이런게 필요하잖아. 이걸 dataSource(커넥션풀을 얻는 방법을 추상화한 인터페이스)에서 얻어옴
        this.template = new NamedParameterJdbcTemplate(dataSource);

        //테이블에 데이터를 넣으려면 테이블명, 칼럼, 넣는 데이터(param)를 알아야함
        //그런데, 우린 dataSource 이용하다보니 메타데이터를 통해 SimpleJdbcInsert가 DB인지함 그 점을 이용한 방식
        this.jdbcInsert = new SimpleJdbcInsert(dataSource)
                .withTableName("item") //테이블명
                .usingGeneratedKeyColumns("id"); //pk명
//                .usingColumns("item_name", "price", "quantity"); //동일해서 생략 가능

    }

    @Override
    public Item save(Item item) {

        //요청 파라미터로 들어온 item 객체를 넣을거니까 => BeanPropertySqlParameterSource
        SqlParameterSource param = new BeanPropertySqlParameterSource(item);

        Number key = jdbcInsert.executeAndReturnKey(param); //keyHolder안넘겨도 알어서 넘겨줌
        item.setId(key.longValue());
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