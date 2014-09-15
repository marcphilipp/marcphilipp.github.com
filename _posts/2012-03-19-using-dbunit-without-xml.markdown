---
layout: post
title: "Using DbUnit Without XML"
date: 2012-03-19 21:00
comments: true
categories: [DbUnit, JUnit]
---

In the [previous post](/blog/2012/03/13/database-tests-with-dbunit-part-1/) we have seen how to use DbUnit to write a simple database test. By using DbUnit for this purpose, we were able to insert a complete dataset into the database without writing SQL. However, we had to use XML to specify the dataset:

{% highlight xml %}
<dataset>
  <PERSON NAME="Bob" LAST_NAME="Doe" AGE="18"/>
  <PERSON NAME="Alice" LAST_NAME="Foo" AGE="23"/>
  <PERSON NAME="Charlie" LAST_NAME="Brown" AGE="42"/>
</dataset>
{% endhighlight %}

Now, you can think about XML what you want but I (and hopefully most people) would rather not want to write such files but instead create the dataset directly in the code of the test class. As it turns out, this is very hard using plain DbUnit.

## DataSetBuilder to the rescue

So, I thought, wouldn't it be nice to have a builder for datasets with an easy-to-use API? Thus, I sat down and wrote [DataSetBuilder](https://github.com/marcphilipp/dbunit-datasetbuilder) for DbUnit.

<!--more-->

Here's how it can be used to replace the `dataset.xml` file from above:

{% highlight java %}
IDataSet buildDataSet() throws DataSetException {
	DataSetBuilder builder = new DataSetBuilder();
	builder.newRow("PERSON").with("NAME", "Bob").with("LAST_NAME", "Doe").with("AGE", 18).add();
	builder.newRow("PERSON").with("NAME", "Alice").with("LAST_NAME", "Foo").with("AGE", 23).add();
	builder.newRow("PERSON").with("NAME", "Charlie").with("LAST_NAME", "Brown").with("AGE", 42).add();
	return builder.build();
}
{% endhighlight %}

Lots of strings you might say. That's what I thought as well. In addition, it's not typesafe at all. You could insert a string into the `AGE` column for example. To circumvent this problem there's the `ColumnSpec<T>` class which represents a column of a certain type `T`.

Using a `ColumnSpec` the first row in the previous example can be written as:

{% highlight java %}
ColumnSpec<Integer> age = ColumnSpec.newColumn("AGE");
builder.newRow("PERSON").with("NAME", "Bob").with("LAST_NAME", "Doe").with(age, 18).add();
{% endhighlight %}

Now, inserting a string into the age column would cause a compile error:

{% highlight java %}
// compile error: method with(...) not applicable for the arguments (ColumnSpec<Integer>, String)
builder.newRow("PERSON").with(age, "18"); 
{% endhighlight %}

You can extract constants for the `ColumnSpec` instances and collect them in a class per table to be shared and used by all your database tests:

{% highlight java %}
class PersonTable {
	static final ColumnSpec<String> NAME = newColumn("NAME");
	static final ColumnSpec<String> LAST_NAME = newColumn("LAST_NAME");
	static final ColumnSpec<Integer> AGE = newColumn("AGE");
}
{% endhighlight %}

After the refactoring, our `buildDataSet` method looks like this (using static imports for the constants):

{% highlight java %}
IDataSet buildDataSet() throws DataSetException {
	DataSetBuilder builder = new DataSetBuilder();
	builder.newRow("PERSON").with(NAME, "Bob").with(LAST_NAME, "Doe").with(AGE, 18).add();
	builder.newRow("PERSON").with(NAME, "Alice").with(LAST_NAME, "Foo").with(AGE, 23).add();
	builder.newRow("PERSON").with(NAME, "Charlie").with(LAST_NAME, "Brown").with(AGE, 42).add();
	return builder.build();
}
{% endhighlight %}

This approach has another advantage: The tests can easily be adapted when a column is renamed. All it takes is changing the string parameter of the `newColumn` method. Now, try to do that with numerous XML files...

To sum up, this post has shown how DbUnit can be used without maintaining XML files. Instead, the datasets are build directly in the test code.

## What's next? 

In the next post of this series on database tests with DbUnit, we will explore how our database tests can be made even more robust against changes of the database schema such as adding or removing a column.
