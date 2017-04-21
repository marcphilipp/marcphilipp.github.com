---
title: "Database Tests With DbUnit (Part 1)"
date: 2012-03-13 21:00
comments: true
categories: [DbUnit, JUnit]
lang: en
ref: post-db-unit-part1
---

Inspired by a recent [blog post](http://blog.schauderhaft.de/2012/01/15/tipps-for-testing-database-code/) and [presentation](http://www.sigs-datacom.de/oop2012/konferenz/sessiondetails.html?tx_mwconferences_pi1%5BshowUid%5D=752&tx_mwconferences_pi1%5Banchor%5D=%23Mi64&tx_mwconferences_pi1%5Bs%5D=0) by [Jens Schauder](http://blog.schauderhaft.de/uber-jens-schauder/), this blog posts starts a series of posts about using [DbUnit](http://www.dbunit.org/) for database and integration tests. In addition, I will talk about [Database Tests with DbUnit](http://www.andrena.de/veranstaltungen/datenbanktests-mit-dbunit) at ObjektForum Karlsruhe in April. This first post introduces DbUnit and demonstrates how it can be used to write database tests.

<!--more-->

For tests that use a database it is crucial to be able to set up the database to a known state before each test run. Otherwise such tests tend to be very fragile and will require a lot of manual care to keep them green over time as they will often fail due to different database contents.

Suppose we want to test a class called `PersonRepository` that loads instances of `Person` from the database. For example, the method `findPersonByFirstName` should load the (first) person with a specified first name. How can we test this method? Our first test might look like this:

```java
@Test
public void findsAndReadsExistingPersonByFirstName() throws Exception {
	PersonRepository repository = new PersonRepository(dataSource());
	Person charlie = repository.findPersonByFirstName("Charlie");

	assertThat(charlie.getFirstName(), is("Charlie"));
	assertThat(charlie.getLastName(), is("Brown"));
	assertThat(charlie.getAge(), is(42));
}
```

A crucial part is missing though: the setup. We need to prepare the database before we can run the test, i.e. we need to make sure the database contains a person called "Charlie" before we can call  `findPersonByFirstName` and check the result.

## Importing a dataset into the database

DbUnit offers a useful approach to solving this problem, e.g. it allows to cleanly insert a data set required by a test into the database. Usually, such a data set is specified in a separate XML file, like this:

```xml
<dataset>
  <PERSON NAME="Bob" LAST_NAME="Doe" AGE="18"/>
  <PERSON NAME="Alice" LAST_NAME="Foo" AGE="23"/>
  <PERSON NAME="Charlie" LAST_NAME="Brown" AGE="42"/>
</dataset>
```

This simple dataset contains three rows of the `PERSON` table which has three columns, namely `NAME`, `LAST_NAME`, and `AGE`.

DbUnit can then read the file into an `IDataSet` like this:

```java
private IDataSet readDataSet() throws Exception {
	return new FlatXmlDataSetBuilder().build(new File("dataset.xml"));
}
```

Next, we tell DbUnit to load our `IDataSet` into the database. In this example, we use an in-memory instance of [H2](http://www.h2database.com/).

```java
private void cleanlyInsertDataset(IDataSet dataSet) throws Exception {
	IDatabaseTester databaseTester = new JdbcDatabaseTester(
		"org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "");
	databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
	databaseTester.setDataSet(dataSet);
	databaseTester.onSetup();
}
```

The `CLEAN_INSERT` operation instructs DbUnit to delete all rows from the `PERSON` table and then insert the rows from our dataset into the database. In our test class, we will import the dataset before each test case using the `@Before` annotation of JUnit:

```java
@Before
public void importDataSet() throws Exception {
	IDataSet dataSet = readDataSet();
	cleanlyInsertDataset(dataSet);
}
```

## Creating the database schema

However, before we can load the dataset into the database, we have to ensure that the database schema has been created. Since we are using a non-persistent H2 instance we can simply create the schema before running our first test case. In JUnit, this can be achieved using the `@BeforeClass` annotation like this:

```java
@BeforeClass
public static void createSchema() throws Exception {
	RunScript.execute("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
	                  "sa", "", "schema.sql", UTF8, false);
}
```

`RunScript` is an utility class provided by H2 that executes the specified SQL file against the specified database. Other databases provide similiar mechanisms to execute SQL files. In our case, the `schema.sql` file looks like this:

```sql
create table if not exists PERSON (
	ID int identity primary key,
	NAME varchar,
	LAST_NAME varchar,
	AGE  smallint,
)
```

## Putting the pieces together

Our test setup is complete. First, we create the database schema. Second, we import three rows from `dataset.xml` into the `PERSON` table. Third, we run our test case.

Here are all the bits and pieces put together:

```java
public class XmlDatabaseTest {

	private static final String JDBC_DRIVER = org.h2.Driver.class.getName();
	private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
	private static final String USER = "sa";
	private static final String PASSWORD = "";

	@BeforeClass
	public static void createSchema() throws Exception {
		RunScript.execute(JDBC_URL, USER, PASSWORD, "schema.sql", UTF8, false);
	}

	@Before
	public void importDataSet() throws Exception {
		IDataSet dataSet = readDataSet();
		cleanlyInsert(dataSet);
	}

	private IDataSet readDataSet() throws Exception {
		return new FlatXmlDataSetBuilder().build(new File("dataset.xml"));
	}

	private void cleanlyInsert(IDataSet dataSet) throws Exception {
		IDatabaseTester databaseTester = new JdbcDatabaseTester(JDBC_DRIVER, JDBC_URL, USER, PASSWORD);
		databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
		databaseTester.setDataSet(dataSet);
		databaseTester.onSetup();
	}

	@Test
	public void findsAndReadsExistingPersonByFirstName() throws Exception {
		PersonRepository repository = new PersonRepository(dataSource());
		Person charlie = repository.findPersonByFirstName("Charlie");

		assertThat(charlie.getFirstName(), is("Charlie"));
		assertThat(charlie.getLastName(), is("Brown"));
		assertThat(charlie.getAge(), is(42));
	}

	@Test
	public void returnsNullWhenPersonCannotBeFoundByFirstName() throws Exception {
		PersonRepository repository = new PersonRepository(dataSource());
		Person person = repository.findPersonByFirstName("iDoNotExist");

		assertThat(person, is(nullValue()));
	}

	private DataSource dataSource() {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL(JDBC_URL);
		dataSource.setUser(USER);
		dataSource.setPassword(PASSWORD);
		return dataSource;
	}
}
```

Of course, there's still some room for improvement. For example, `createSchema()`, `cleanlyInsert()`, and `dataSource()` have yet to be made reusable by other test classes. However, this would destroy the self-containedness of the test which is important for this blog post. ;-)

## What's next?

In the next post, we will see how the test data (now hidden away in `dataset.xml`) can be moved into the test code. Stay tuned!
