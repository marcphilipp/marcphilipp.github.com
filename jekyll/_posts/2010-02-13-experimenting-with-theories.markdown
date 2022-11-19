---
title: "Experimenting with Theories"
date: 2010-02-13 14:04
comments: true
categories: [JUnit]
lang: en
ref: post-experimenting-with-theories
---

The very first 4.x release of JUnit contained support for custom test runners. Moreover, it came with the `Parameterized` test runner that allows to execute the test cases in a test class against a collection of values, i.e. parameters.

<!--more-->

The example that comes with the Javadoc of the `Parameterized` class tests an imaginary Fibonacci calculator for a number of data points:

```java
@RunWith(Parameterized.class)
public class FibonacciParameterizedTest {

    @Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[][] { { 0, 0 }, { 1, 1 },
                { 2, 1 }, { 3, 2 }, { 4, 3 }, { 5, 5 }, { 6, 8 } });
    }

    private final int input;
    private final int expected;

    public FibonacciParameterizedTest(int input, int expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() {
        assertEquals(expected, Fibonacci.compute(input));
    }
}
```

JUnit 4.4 introduced Theories. A theory is an abstraction of a concrete test scenario, i.e. while a test specifies the behavior in one particular case, a theory captures more than a single scenario but is usually not as detailed in its assertions.

When using a Parameterized test you usually specify the input along with the expected output. Of course, you can do the same with a theory. However, theories allow for a complete different approach to testing the Fibonacci calculator.

E.g. a simple theory could state that for one of the seeds, i.e. 0 or 1, the same number is returned as result. Another theory could test the recurrence relation, i.e. that `Fibonacci(n)` always equals `Fibonacci(n-1)` + `Fibonacci(n-2)`. In Java this can be written as:

```java
@RunWith(Theories.class)
public class FibonacciTheories {

    @DataPoints
    public static int[] VALUES = { 0, 1, 2, 3, 4, 5, 6 };

    @Theory
    public void seeds(int n) {
        assumeTrue(n <= 1);
        assertEquals(n, compute(n));
    }

    @Theory
    public void recurrence(int n) {
        assumeTrue(n > 1);
        assertEquals(compute(n - 1) + compute(n - 2), compute(n));
    }
}
```

Even shorter yet:

```java
@RunWith(Theories.class)
public class FibonacciTheories {

    @Theory
    public void seeds(@TestedOn(ints = { 0, 1 }) int n) {
        assertEquals(n, compute(n));
    }

    @Theory
    public void recurrence(@TestedOn(ints = { 2, 3, 4, 5, 6 }) int n) {
        assertEquals(compute(n - 1) + compute(n - 2), compute(n));
    }
}
```

So, which test is better? Actually, I am still undecided. While theories certainly look more elegant, a parameterized tests states its assumptions more clearly. The answer seems to be, as always: It depends.
