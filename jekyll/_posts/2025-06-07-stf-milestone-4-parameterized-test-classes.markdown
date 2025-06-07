---
title: "STF Milestone 4: Parameterized test classes"
date: 2025-06-07 00:01
categories: [Sovereign Tech Fund, JUnit 5]
lang: en
ref: 2025-06-07-stf-milestone-4-parameterized-test-classes
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

In version 5.0, JUnit Jupiter introduced support for parameterizing test methods.
In JUnit 4, only test classes could be parameterized.
Therefore, being able to do so on the method level provided more flexibility.
However, there are cases where a set of tests should be executed against the same sets of arguments.
The recent [5.13 release](https://github.com/junit-team/junit5/releases/tag/r5.13.0) introduced support for `@ParameterizedClass` thereby finally resolving this highly-voted feature request.<!--more-->
{: .lead}

To write a parameterized test _method_, the `@ParameterizedTest` annotation is used instead of `@Test`.
In addition, at least one `@...Source` annotation is required to specify the _source_ of argument sets that the method will be invoked with.

```java
class SomeTests {
    @ParameterizedTest
    @ValueSource(strings = {"foo", "bar"})
    void shouldNotBeNull(String value) {
        assertNotNull(value);
    }
    @ParameterizedTest
    @ValueSource(strings = {"foo", "bar"})
    void lengthShouldBeThree() {
        assertEquals(3, value.length());
    }
}
```

In the above example, both test methods are parameterized with an identical `@ValueSource` annotation.
Each will be invoked twice, once with `"foo"` and once with `"bar"` as its `String` parameter.

Since both test methods use the same parameters, parameterizing the test class allows to remove the duplication.
To do so, the test class is annotated with the new `@ParameterizedClass` annotation and the `@...Source` annotation.
The test methods are changed to use the `@Test` annotation.

```java
@ParameterizedClass
@ValueSource(strings = {"foo", "bar"})
class SomeTests {
    @Parameter String value;
    
    @Test
    void shouldNotBeNull() {
        assertNotNull(value);
    }
    @Test
    void lengthShouldBeThree() {
        assertEquals(3, value.length());
    }
}
```

Instead of declaring the parameter on the methods, JUnit injects it into the `@Parameter`-annotated field.
Alternatively, JUnit also supports [constructor injection](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests-consuming-arguments-classes).

```java
@ParameterizedClass
@ValueSource(strings = {"foo", "bar"})
record SomeTests(String value) {
    @Test
    void shouldNotBeNull() {
        assertNotNull(value);
    }
    @Test
    void lengthShouldBeThree() {
        assertEquals(3, value.length());
    }
}
```

In the example above, a Java _record_ is used and the class-level parameters are specified as record components.
This allows reducing the boilerplate of declaring a constructor and assigning its parameters to fields.
Regular classes may be used as well, though.

## A common usecase

A common use case for parameterizing a test class is to run its tests against different implementations of an interface.
Prior to JUnit 5.13, one would typically achieve that by writing an abstract base test class and creating a subclass for each concrete implementation.
For example, the following test class implements two tests against Java's `List` interface that are executed against three implementations of `List` that are part of the JDK.

```java
public class ListTests {

    private static abstract class AbstractListTests {

        List<String> list;

        @BeforeEach
        void initializeList() {
            list = createList();
        }

        protected abstract <T> List<T> createList();

        @Test
        void newListIsEmpty() {
            assertTrue(list.isEmpty());
        }

        @Test
        void itemCanBeAdded() {
            var added = list.add("value");

            assertTrue(added);
            assertTrue(list.contains("value"));
            assertEquals("value", list.getFirst());
        }
    }

    @Nested
    @DisplayName("ArrayList")
    class ArrayListTests extends AbstractListTests {
        @Override
        protected <T> List<T> createList() {
            return new ArrayList<>();
        }
    }

    @Nested
    @DisplayName("LinkedList")
    class LinkedListTests extends AbstractListTests {
        @Override
        protected <T> List<T> createList() {
            return new LinkedList<>();
        }
    }

    @Nested
    @DisplayName("Vector")
    class VectorTests extends AbstractListTests {
        @Override
        protected <T> List<T> createList() {
            return new Vector<>();
        }
    }
}
```

This test can be rewritten using `@ParameterizedClass` and a `@MethodSource` as follows.

```java
@ParameterizedClass
@MethodSource("listImplementations")
public class ParameterizedListTests {

    static Stream<?> listImplementations() {
        return Stream.of(
                argumentSet("ArrayList", new ArrayList<>()),
                argumentSet("LinkedList", new LinkedList<>()),
                argumentSet("Vector", new Vector<>())
        );
    }

    @Parameter
    List<String> list;

    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
    }

    @Test
    void itemCanBeAdded() {
        var added = list.add("value");

        assertTrue(added);
        assertTrue(list.contains("value"));
        assertEquals("value", list.getFirst());
    }

    @AfterEach
    void clearList() {
        // Necessary since the `List` parameter is mutable!
        list.clear();
    }
}
```

This allows to get rid of the abstract base class and subclasses resulting in a much simpler structure.

## Converters

The same use case can also be implemented using `@ValueSource` (and constructor injection) instead of `@MethodSource` as follows.

```java
@ParameterizedClass(name = "[{index}] {0}")
@ValueSource(classes = {ArrayList.class, LinkedList.class, Vector.class})
public class ParameterizedWithValueSourceListTests {

    List<String> list;

    ParameterizedWithValueSourceListTests(Class<? extends List<String>> listType) {
        this.list = ReflectionSupport.newInstance(listType);
    }
    
    @Test
    void newListIsEmpty() {
        // same as above...
    }

    @Test
    void itemCanBeAdded() {
        // same as above...
    }
}
```

Since `@ValueSource` only supports class literals, not concrete instances, the constructor relies on JUnit's `ReflectionSupport` class to instantiate the `List` via the implementations' default constructors.
Whether it's ok to use reflection here is, of course, debatable.
Personally, I think it's acceptable in this case since test code is executed with every build and would fail right away.

If you find yourself using such a _conversion_ in multiple places, it might make sense to extract it into a custom `ArgumentConverter` and annotate the parameter with `@ConvertWith` instead.

```java
@ParameterizedClass(name = "[{index}] {0}")
@ValueSource(classes = {ArrayList.class, LinkedList.class, Vector.class})
public class ParameterizedWithValueSourceAndConverterListTests {

    @Parameter
    @ConvertWith(ClassToInstanceConverter.class)
    List<String> list;

    @Test
    void newListIsEmpty() {
        // same as above...
    }

    @Test
    void itemCanBeAdded() {
        // same as above...
    }

    @AfterEach
    void clearList() {
        // Necessary since the `List` parameter is mutable!
        list.clear();
    }
}
```

The custom `ArgumentConverter` implementation is called `ClassToInstanceConverter` in this example and contains the code that was part of the constructor in the previous example.

```java
class ClassToInstanceConverter extends SimpleArgumentConverter {
    @Override
    protected Object convert(Object source, Class<?> targetType) {
        return ReflectionSupport.newInstance((Class<?>) source);
    }
}
```

You can even make this shorter by utilizing JUnit's support for composed annotations.

```java
@Retention(RUNTIME)
@Target({PARAMETER, FIELD})
@ConvertWith(ClassToInstanceConverter.class)
@interface Instantiate {
}
```

Of course, that also works with constructor injections, for example, using a Java record.

```java
@ParameterizedClass(name = "[{index}] {0}")
@ValueSource(classes = {ArrayList.class, LinkedList.class, Vector.class})
public record ParameterizedWithValueSourceAndConverterRecordListTests(
        @Instantiate List<String> list) {

    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
    }

    @Test
    void itemCanBeAdded() {
        var added = list.add("value");

        assertTrue(added);
        assertTrue(list.contains("value"));
        assertEquals("value", list.getFirst());
    }

    @AfterEach
    void clearList() {
        // Necessary since the `List` parameter is mutable!
        list.clear();
    }
}
```

## Mutable vs. immutable arguments

When using _mutable_ data as parameters, one has to be mindful of tests that change state.
In the above example, if `itemCanBeAdded()` were to run before `newListIsEmpty()` without the `@AfterEach` lifecycle method, `newListIsEmpty()` would fail because the `list` would no longer be empty.
Therefore, it's usually a better idea to use _immutable_ data as parameters.
In this case, this can be achieved by using a `Supplier` to create a list in the test class constructor.
Since each test method uses a separate instance of the test class (unless `@TestInstance(PER_CLASS)` is used), this will prevent test methods from influencing each other.
This also removes the need to reset the state of the parameters in an `@AfterEach` lifecycle method.

```java
@ParameterizedClass
@MethodSource("listImplementations")
public class ParameterizedListWithSuppliersTests {

    static Stream<?> listImplementations() {
        return Stream.of(
                argumentSet("ArrayList", (Supplier<?>) ArrayList::new),
                argumentSet("LinkedList", (Supplier<?>) LinkedList::new),
                argumentSet("Vector", (Supplier<?>) Vector::new)
        );
    }

    final List<String> list;

    ParameterizedListWithSuppliersTests(Supplier<List<String>> listSupplier) {
        this.list = listSupplier.get();
    }

    @Test
    void newListIsEmpty() {
        // same as above...
    }

    @Test
    void itemCanBeAdded() {
        // same as above...
    }
}
```

## Argument sources

In addition to `@ValueSource` and `@MethodSource`, JUnit provides the following annotations for specifying the sources of arguments:

- `@EnumSource`, `@NullSource`, and `@EmptySource` for simple values
- `@CsvSource`, `@CsvFileSource` for CSV content
- `@FieldSource` for custom code
- `@ArgumentsSource(MyProvider.class)` for custom providers

Please refer to JUnit's [User Guide](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests-sources) for details.

## Lifecycle methods

Parameterized classes may declare `@BeforeParameterizedClassInvocation` and `@AfterParameterizedClassInvocation` lifecycle methods which are called once before/after each invocation of the parameterized class with a set of arguments.
This may be used, for example, to initialize an argument as demonstrated in the following example.

```java
@ParameterizedClass(name = "{0}")
@ValueSource(classes = {ArrayList.class, LinkedList.class, Vector.class})
public record CustomInitializationListTests(
        @Instantiate List<String> list) {

    @BeforeParameterizedClassInvocation
    static void before(List<String> list) {
        if (list instanceof ArrayList<?> arrayList) {
            arrayList.ensureCapacity(100);
        }
    }

    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
    }

    @Test
    void itemCanBeAdded() {
        var added = list.add("value");

        assertTrue(added);
        assertTrue(list.contains("value"));
        assertEquals("value", list.getFirst());
    }

    @AfterEach
    void clearList() {
        list.clear();
    }
}
```

## Cartesian products

The `@ParameterizedClass` annotation may be combined with `@Nested`, also within an enclosing `@ParameterizedClass`-annotated test class.
Moreover, a `@ParameterizedClass` may contain `@ParameterizedTest` methods.
Both can be used to test all combinations or, mathematically speaking, the Cartesian product of two parameter lists.

The following example demonstrates that by testing all three concrete list implementations from the previous examples against each other in the `@Nested` `Interoperability` test class.
The `@Nested` class additionally contains a `@ParameterizedTest` method  to test against different `String` values.

```java

@ParameterizedClass(name = "[{index}] {0}")
@ValueSource(classes = {ArrayList.class, LinkedList.class, Vector.class})
public class ParameterizedWithNestedListTests {

    @Parameter
    @Instantiate
    List<String> list;

    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
    }

    @Test
    void itemCanBeAdded() {
        var added = list.add("value");

        assertTrue(added);
        assertTrue(list.contains("value"));
        assertEquals("value", list.getFirst());
    }

    @AfterEach
    void clearList() {
        list.clear();
    }

    @Nested
    @ParameterizedClass(name = "[{index}] {0}")
    @ValueSource(classes = {ArrayList.class, LinkedList.class, Vector.class})
    class Interoperability {

        @Parameter
        @Instantiate
        List<String> secondList;

        @Test
        void twoListsWithSameItemsAreEqual() {
            list.add("foo");
            list.add("bar");

            secondList.addAll(list);

            assertEquals(secondList, list);
        }

        @ParameterizedTest
        @ValueSource(strings = {"baz", "qux"})
        void removeAllItemsInPassedList(String extraItem) {
            list.add("foo");
            list.add("bar");

            secondList.add("foo");
            secondList.add(extraItem);

            list.removeAll(secondList);

            assertEquals(1, list.size());
            assertEquals("bar", list.getFirst());
        }

        @AfterEach
        void clearList() {
            secondList.clear();
        }
    }
}
```

A word of warning: the number of combinations can quickly become very large!
Therefore, you should take that into consideration when deciding whether to use this feature.

## Summary

Parameterized test classes are a powerful testing tool that has long been missing from JUnit Jupiter.
I'm super happy that I've finally had the chance to resolve this long-standing and highly-voted issue thanks to the [Sovereign Tech Fund](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/).

---

Edit: Thanks to my fellow JUnit 5 co-maintainer [Sam Brannen](https://github.com/sbrannen) for his feedback which I applied after initially publishing this post.
