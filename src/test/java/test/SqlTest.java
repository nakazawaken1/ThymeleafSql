package test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import main.Sql;

@SuppressWarnings("javadoc")
public class SqlTest {
	@BeforeAll
	static void before() {
		new Sql("create.sql").executeBatch();
	}

	@AfterAll
	static void after() {
		new Sql("drop.sql").executeBatch();
	}

	@Test
	void testAll() {
		Sql sql = new Sql("select.sql");
		assertEquals(1, sql.queryOriginal().size());
		assertEquals(3, sql.query().size());
		assertEquals(2, sql.set("name", "%太郎").query().size());
		assertEquals(1, sql.reset().set("age", 30).query().size());
		assertEquals(0, sql.reset().set("name", "%太郎").set("age", 30).query().size());
	}
}
