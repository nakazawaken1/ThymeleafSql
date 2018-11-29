package main;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@SuppressWarnings("javadoc")
public class Sql {
	public static void main(String[] args) {
		new Sql("create.sql").executeBatch();
		try {
			Sql sql = new Sql("select.sql");
			System.out.println(sql.queryOriginal());
			System.out.println(sql.query());
			System.out.println(sql.set("name", "%太郎").query());
			System.out.println(sql.reset().set("age", 30).query());
			System.out.println(sql.reset().set("name", "%太郎").set("age", 30).query());
		} finally {
			new Sql("drop.sql").executeBatch();
		}
	}

	String template;
	
	Map<String, Object> parameterMap;

	static Consumer<Object> log = System.out::println;
	
	static final NamedParameterJdbcTemplate jdbc;
	static {
		try {
			@SuppressWarnings("unchecked")
			Class<? extends DataSource> c = (Class<? extends DataSource>) Class
					.forName(System.getProperty("datasource", "com.mysql.cj.jdbc.MysqlDataSource"));
			DataSource dataSource = c.getConstructor().newInstance();
			c.getMethod("setUrl", String.class).invoke(dataSource, System.getProperty("url",
					"jdbc:mysql://localhost/test?user=root&password=&characterEncoding=utf8&serverTimezone=JST"));
			jdbc = new NamedParameterJdbcTemplate(dataSource);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static final TemplateEngine engine;
	static {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resolver.setTemplateMode(TemplateMode.JAVASCRIPT);
		resolver.setSuffix("");
		resolver.setCacheable(false);
		engine = new TemplateEngine();
		engine.addTemplateResolver(resolver);
	}

	public Sql(String template) {
		this.template = template;
		parameterMap = new LinkedHashMap<>();
	}

	public Sql set(String name, Object value) {
		parameterMap.put(name, value);
		return this;
	}

	public Sql reset() {
		parameterMap.clear();
		return this;
	}

	public String build() {
		return engine.process(template, new Context(null, parameterMap));
	}

	public List<Map<String, Object>> query() {
		String sql = build();
		log.accept(parameterMap);
		log.accept(sql);
		return jdbc.queryForList(sql, parameterMap);
	}

	public List<Map<String, Object>> queryOriginal() {
		try (Scanner scanner = new Scanner(ClassLoader.getSystemResourceAsStream(template),
				StandardCharsets.UTF_8.name())) {
			scanner.useDelimiter("^Z");
			String sql = scanner.next();
			log.accept(sql);
			return jdbc.getJdbcOperations().queryForList(sql);
		}
	}

	public int execute() {
		String sql = build();
		log.accept(parameterMap);
		log.accept(sql);
		return jdbc.update(sql, parameterMap);
	}

	public int[] executeBatch() {
		String[] sqls = build().split(";\\s*\r?\n");
		for (String sql : sqls)
			log.accept(sql);
		return jdbc.getJdbcOperations().batchUpdate(sqls);
	}
}
