package juton113.Medicall.support;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// N+1 검증용 쿼리 카운트 테스트 지원 클래스. Hibernate Statistics로 실행된 SQL 쿼리 수를 센다.
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
public abstract class QueryCountTestSupport {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUpQueryCounter() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    protected void resetQueryCount() {
        statistics.clear();
    }

    // getQueryExecutionCount()은 명시적 HQL/JPQL 쿼리만 세고 save()/persist()가 유발하는
    // INSERT/UPDATE는 잡지 못하므로, 실제 DB 왕복 횟수를 온전히 반영하는
    // getPrepareStatementCount()(JDBC PreparedStatement 생성 수)를 기준으로 삼는다.
    protected long getQueryCount() {
        return statistics.getPrepareStatementCount();
    }
}
