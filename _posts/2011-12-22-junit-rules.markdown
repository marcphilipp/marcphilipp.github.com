---
layout: post
title: "JUnit Rules"
date: 2011-12-22 14:24
comments: true
categories: [JUnit]
---

Marc Philipp, andrena objects ag  
Stefan Birkner, Immobilien Scout GmbH  
_Erschienen in Java aktuell, 1/2012, dem Magazin der [iJUG](http://www.ijug.eu/)._

Automatisierte Tests sind aus der heutigen Softwareentwicklung nicht mehr wegzudenken. JUnit ist das älteste und bekannteste Testing-Framework für Java. Doch selbst ein so etabliertes und einfach zu benutzendes Framework wird kontinuierlich weiterentwickelt. Eine der Neuerungen sind JUnit Rules, die Entwicklern eine neue mächtige Möglichkeit bieten, Tests zu formulieren und besser zu strukturieren.<!--more-->
{: .lead}

Der Legende nach haben Kent Beck und Erich Gamma 1997 den Kern von JUnit auf dem Weg zu einer Konferenz im Flugzeug zwischen Zürich und Atlanta geschrieben. JUnit griff die Idee wieder auf, die Beck 1994 mit SUnit [[1][BeckSmalltalkTesting]] für Smalltalk eingeführt hatte: ein Testing-Framework, dessen Zielgruppe Programmierer sind, also dieselben Leute, die auch den Code schreiben, den es zu testen gilt. JUnit ist inzwischen weit verbreitet. Es wird nicht nur zum Schreiben von Unittests, sondern auch zur Automatisierung von Integrations- und Akzeptanztests verwendet.

Viele erfolgreiche Open-Source-Projekte zeichnen sich dadurch aus, dass mit der Zeit immer neue Features eingebaut werden. Dies führt häufig dazu, dass einst simple Bibliotheken unübersichtlich und schwer wartbar werden. JUnit geht hier gezielt einen anderen Weg. David Saff, neben Kent Beck der zweite Maintainer von JUnit, sieht das so: „JUnit is the intersection of all possible useful Java test frameworks, not their union”.

Die Wahrnehmung in der Java-Entwicklergemeinde ist dementsprechend: Da JUnit so einfach ist, meint jeder, der es schon einmal benutzt hat, es gut zu kennen. Das ist einerseits gut, denn die Hürde Unittests zu schreiben ist so sehr niedrig. Andererseits führt es dazu, dass Neuerungen von vielen Entwicklern gar nicht oder erst verzögert wahrgenommen werden. Fragt man Entwicklerkollegen nach Neuerungen in JUnit, wird häufig die Umstellung von Vererbung auf Annotations-basierte Testschreibweise in Version 4.0 erwähnt.

Seitdem hat sich allerdings einiges getan. Die neueste Innovation, die mit Version 4.7 eingeführt wurde, heißt Rules. Zugegeben, unter dem Begriff kann man sich erst einmal nichts vorstellen. Hat man sich diese „Regeln” für Tests aber einmal eingehend angesehen -- und genau das werden wir in diesem Artikel tun -- stellt man fest: Rules werden die Art, wie wir JUnit-Tests schreiben, nachhaltig verändern.


## Was sind Rules?

Mithilfe von JUnit-Rules lässt sich die Ausführung von Tests beeinflussen. Ähnlich einem Aspekt in der aspektorientierten Programmierung (AOP) kann die Rule Code vor, nach oder anstelle einer Testmethode ausführen [[2][JensSchauderBlog]]. Hinter dieser abstrakten Beschreibung steckt ein mächtiges Werkzeug, wie die folgenden Beispiele zeigen.


## Standard-Rules

JUnit selbst liefert fünf Rules mit, an denen wir den praktischen Einsatz zeigen (der Quellcode aller Beispiele ist auf GitHub verfügbar [[3][GitHubPage]]).

### Temporäre Dateien

Beim Testen von Code, der Dateioperationen ausführt, steht man häufig vor dem Problem, dass der Test temporär eine Datei benötigt, die nach dem Test wieder gelöscht werden soll. Bisher brachte man den entsprechenden Code in @Before- und @After-Methoden unter, wie das folgende Beispiel zeigt.

```java
public class TemporaryFolderWithoutRule {
	private File folder;

	@Before
	public void createTemporaryFolder() throws Exception {
		folder = File.createTempFile("myFolder", "");
		folder.delete();
		folder.mkdir();
	}

	@Test
	public void test() throws Exception {
		File file = new File(folder, "test.txt");
		file.createNewFile();
		assertTrue(file.exists());
	}

	@After
	public void deleteTemporaryFolder() {
		recursivelyDelete(folder);
	}

	private void recursivelyDelete(File file) {
		File[] files = file.listFiles();
		if (files != null) {
			for (File each : files) {
				recursivelyDelete(each);
			}
		}
		file.delete();
	}
}
```

Dieser Test kann mit der `TemporaryFolder`-Rule wesentlich kürzer und prägnanter formuliert werden, da die Rule den Framework-Code kapselt.

Um die Rule zu verwenden, muss innerhalb des Tests ein Feld vom Typ `TemporaryFolder` angelegt werden. Dieses Feld muss `public` sein und mit der Annotation `@Rule` markiert werden, sodass JUnit die Rule erkennt. So markierte Rules wirken sich auf die Ausführung aller Testmethoden einer Testklasse aus.

```java
public class TemporaryFolderWithRule {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void test() throws Exception {
		File file = folder.newFile("test.txt");
		assertTrue(file.exists());
	}
}
```

Die Testmethode `test()` legt mithilfe der `TemporaryFolder`-Rule die Datei `test.txt` an und überprüft danach, dass die Datei erzeugt wurde. Doch wo wurde die Datei erzeugt? Der Name `TemporaryFolder` suggeriert es bereits: in einem temporären Ordner. Doch die Rule legt die Datei nicht nur an, sondern löscht sie nach dem Test auch wieder, inklusive des temporären Ordners.

### Timeout

Es kommt gelegentlich vor, dass man Code schreibt, der versehentlich Endlosschleifen enthält. Ein JUnit-Test, der diese Codestellen testet, läuft in diese Endlosschleifen. Bei Verwendung der `Timeout`-Rule schlagen solche Tests fehl, da sie nicht innerhalb der vorgegebenen Zeit beendet werden.

```java
public class GlobalTimeout {

	@Rule
	public Timeout timeout = new Timeout(20); //timeout nach 20 ms

	@Test
	public void firstTest() {
		while (true) {}
	}

	@Test
	public void secondTest() {
		for (;;) {}
	}
}
```

Führt man diesen Test aus, schlagen beide Testmethoden fehl. Würde man die Rule nicht verwenden, liefe dieser Test endlos.

Wer bisher den `timeout`-Parameter der `@Test`-Annotation verwendet hat, kann diesen durch die `Timeout`-Rule ersetzen. Die Rule bietet den Vorteil, dass sie nur einmal in der Klasse definiert werden muss und dann für alle Testmethoden gilt.

### Erwartete Exceptions

Schon bisher kann das Auftreten von Exceptions mit dem `expected`-Parameter der `@Test`-Annotation getestet werden. Die `ExpectedException`-Rule erweitert die Test-Möglichkeiten für Exceptions. Damit lassen sich neben der Klasse auch die Message und mittels Hamcrest-Matchern sogar beliebige Details der geworfenen Exception testen.

```java
public class ExpectedExceptionWithRule {

	int[] threeNumbers = { 1, 2, 3 };

	@Rule public ExpectedException thrown = ExpectedException.none();

	@Test
	public void exception() {
		thrown.expect(ArrayIndexOutOfBoundsException.class);
		threeNumbers[3] = 4;
	}

	@Test
	public void exceptionWithMessage() {
		thrown.expect(ArrayIndexOutOfBoundsException.class);
		thrown.expectMessage("3");
		threeNumbers[3] = 4;
	}
}
```

### Fehler sammeln

Üblicherweise bricht ein Test nach der ersten fehlgeschlagenen Assertion ab. Will man in einem Test trotzdem alle Assertions abarbeiten, kann man den `ErrorCollector` verwenden. Er sammelt fehlgeschlagene Assertions innerhalb einer Testmethode und gibt am Ende eine Liste der Fehlschläge aus. So kann man etwa alle Elemente in einer Liste überprüfen und den Test erst am Ende fehlschlagen lassen, wenn die Überprüfung eines oder mehrerer Elemente fehlgeschlagen ist.

```java
public class ErrorCollectingTest {

	@Rule
	public ErrorCollector collector = new ErrorCollector();

	@Test
	public void test() {
		collector.checkThat(1 + 1, is(3));
		collector.addError(new Exception("something went wrong"));
	}
}
```

Wenn man diesen Test ausführt, erhält man zwei Fehlernachrichten mit jeweils einem Stacktrace, der einen zu der Zeile im Programmcode führt, wo die Überprüfung fehlgeschlagen ist.

### Testname

Um innerhalb einer Testmethode auf deren Namen zuzugreifen, kann man die `TestName`-Rule verwendet.

```java
public class NameRuleTest {
	@Rule
	public TestName test = new TestName();

	@Test
	public void test() {
		assertThat(test.getMethodName(), is("test"));
	}
}
```


## Rules selber schreiben

Die von JUnit bereitgestellten Rules sind nur der Anfang. Wer sich das Schreiben von Tests erleichtern will, kann seine eigenen Rules schreiben. Das sind letztendlich Klassen, die das Interface `TestRule` mit der Methode `apply(...)` implementieren. Für die häufigsten Anwendungsfälle greift uns JUnit unter die Arme und stellt die drei Templateklassen `ExternalResource`, `TestWatcher` und `Verifier` zur Verfügung.

### Bereitstellung externer Ressourcen

Vielfach werden, insbesondere bei Integrationstests, externe Ressourcen wie Dateien, Server oder Verbindungen benötigt. Diese müssen dem Test zur Verfügung gestellt und nach dessen Ausführung wieder aufgeräumt werden.

Dieses Ressourcenhandling lässt sich recht einfach mit einer Rule abbilden, indem man von der Basisklasse `ExternalResource` ableitet. In der neuen Rule überschreibt man die `before()`-Methode, um die Ressource bereitzustellen, und die `after()`-Methode um sie nach dem Test wieder aufzuräumen. Ein Beispiel hierfür ist die `TemporaryFolder`-Rule, die in der `before()`-Methode ein neues Verzeichnis erstellt und es in der `after()`-Methode wieder löscht.

Wie einfach sich eine solche Rule schreiben lässt, demonstriert das folgende Beispiel. Möchte man für einen Test sicherstellen, dass eine System Property einen bestimmten Wert hat und nach dem Test der alte Wert wiederhergestellt wird, könnte man die Methoden `before()` und `after()` wie folgt implementieren:

```java
public class ProvideSystemProperty extends ExternalResource {

	private final String key, value;
	private String oldValue;

	public ProvideSystemProperty(String key, String value) {
		this.key = key;
		this.value = value;
	}

	@Override
	protected void before() {
		oldValue = System.getProperty(key);
		System.setProperty(key, value);
	}

	@Override
	protected void after() {
		if (oldValue == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, oldValue);
		}
	}
}
```

Und schon kann man die Rule in einem Test verwenden:

```java
public class SomeTestUsingSystemProperty {

	@Rule
	public ProvideSystemProperty property = new ProvideSystemProperty("someKey", "someValue");

	@Test
	public void test() {
		assertThat(System.getProperty("someKey"), is("someValue"));
	}
}
```

### Benachrichtigung über die Testausführung

Da man mit einer Rule Code vor und nach dem Aufruf der Testmethoden ausführen kann, lässt sich damit eine Benachrichtigung über die Testausführung realisieren. Dazu stellt JUnit die abstrakte Oberklasse `TestWatcher` bereit. Diese besitzt vier leer implementierte Methoden, die man nach Bedarf überschreiben kann: `starting()`, `succeeded()`, `failed()` und `finished()`:

```java
public class BeepOnFailure extends TestWatcher {

	@Override
	protected void failed(Throwable e, Description description) {
		Toolkit.getDefaultToolkit().beep();
	}
}
```

Die Benutzung in einem Test sieht dann so aus:

```java
public class FailingTestThatBeeps {

	@Rule
	public BeepOnFailure beep = new BeepOnFailure();

	@Test
	public void test() {
		fail();
	}
}
```

### Überprüfungen nach den Tests

Das dritte von JUnit zur Verfügung gestellte Template ist der `Verifier`. Dort kann man die Methode `verify()` überschreiben, die nach jedem erfolgreichen Test ausgeführt wird. In dieser Methode lassen sich zusätzliche Überprüfungen unterbringen, die im Fehlerfall eine Exception werfen, um den Test doch noch scheitern zu lassen.

Eine Beispielimplementierung von `Verifier` ist der weiter oben vorgestellte `ErrorCollector`. Während des Testlaufs sammelt er alle fehlgeschlagenen Assertions und wirft im Fehlerfall eine `MultipleFailureException` am Ende des Tests.

### TestRule implementieren

Anstatt eines der Templates zu verwenden kann man das Interface `TestRule` auch direkt implementieren. Dieses Interface hat genau eine Methode

```java
Statement apply(Statement base, Description description);
```

Das erste Argument `base` kapselt den auszuführenden Test, der sich mittels `evaluate()` ausführen lässt. Die `description` stellt Informationen zum Test zu Verfügung (bspw. den Testnamen). Der Rückgabewert der Methode ist ein `Statement` dass anstelle des Tests ausgeführt wird. Üblicherweise delegiert das neue `Statement` den Aufruf von `evaluate()` an den ursprünglichen Test und führt zusätzlich weitere Methoden aus. Der folgende Code zeigt beispielhaft die leicht abgewandelte Implementierung des `ExternalResource`-Templates.

```java
public Statement apply(Statement base, Description description) {
	return new Statement() {
		@Override
		public void evaluate() throws Throwable {
			before();
			try {
				base.evaluate();
			} finally {
				after();
			}
		}
	};
}
```

Hier wird zuerst die Template-Methode `before()` ausgeführt, dann der Test selbst mittels `base.evaluate()` und zum Schluss die zweite Template-Methode `after()`.

## Regeln auf Klassenebene

Alle Rules, die wir bisher gesehen haben, wurden für jede Methode einzeln angewandt, genauso wie Methoden, die mit `@Before` und `@After` annotiert sind, vor bzw. nach jedem Test ausgeführt werden. Manchmal möchte man allerdings die Möglichkeit haben, Code nur einmal vor der ersten bzw. nach der letzten Testmethode in einer Klasse auszuführen. Ein häufiger Anwendungsfall sind Integrationstests, die eine Verbindung zu einem Server aufbauen und wieder schließen müssen. Das war bisher nur mit den Annotations `@BeforeClass` bzw. `@AfterClass` möglich, Rules konnte man dazu nicht verwenden. Um dieses Problem zu lösen, wurde in JUnit 4.9 die `@ClassRule`-Annotation eingeführt.

Um eine `ClassRule` zu verwenden, annotiert man ein Feld in der Testklasse, das analog zu `@BeforeClass`-/`@AfterClass`-Methoden `public` und `static` sein muss. Der Typ des Feldes muss wie bei der `@Rule`-Annotation das `TestRule`-Interface implementieren. Eine solche Rule lässt sich nicht nur in einer normalen Testklasse verwenden, sondern auch in einer Test-Suite, wie das folgende Beispiel [[4][ReleaseNotes4.9]] illustriert:

```java
@RunWith(Suite.class)
@SuiteClasses({A.class, B.class, C.class})
public class UsesExternalResource {
	public static Server myServer = new Server();

	@ClassRule
	public static ExternalResource connection = new ExternalResource() {

		@Override protected void before() throws Throwable {
			myServer.connect();
		};

		@Override protected void after() {
			myServer.disconnect();
		};
	};
}
```


## Mehrere Regeln kombinieren

Einen weiteren Vorteil von Rules gegenüber Hilfsmethoden in Testoberklassen stellt ihre Kombinierbarkeit dar. Es lassen sich beliebig viele Rules in einem Test verwenden:

```java
public class CombiningMultipleRules {

	@Rule public TestRule beep = new BeepOnFailure();
	@Rule public ExpectedException exceptions = ExpectedException.none();
	@Rule public TestName test = new TestName();

	@Test
	public void test() {
		exceptions.expect(IllegalArgumentException.class);
		throw new RuntimeException("Hello from " + test.getMethodName());
	}
}
```

Das funktioniert wunderbar, solange die Rules voneinander unabhängig sind. JUnit macht absichtlich keinerlei Zusicherungen was die Reihenfolge der Abarbeitung von Rules angeht [[5][KentBeckRuleChain]]. Manchmal möchte man aber dennoch eine bestimmte Reihenfolge vorgeben. Angenommen man hat zwei Rules, von denen die erste eine bestimmte Ressource zur Verfügung stellt, die von der zweiten Rule benutzt wird. Dann möchte man sehr wohl sicherstellen, dass zuerst die Ressource bereitgestellt wird, bevor sie konsumiert wird. Dafür wurde in JUnit 4.10 die `RuleChain`-Klasse eingeführt. `RuleChain` implementiert selbst das `TestRule`-Interface, kann also verwendet werden, wie eine normale Rule [[6][ReleaseNotes4.10]]:

```java
public class UseRuleChain {
	@Rule
	public TestRule chain = RuleChain.outerRule(new LoggingRule("outer rule"))
			.around(new LoggingRule("middle rule"))
			.around(new LoggingRule("inner rule"));
	@Test
	public void test() {}
}
```

Wenn man diesen Test ausführt, erhält man folgende Ausgabe:

	starting outer rule
	starting middle rule
	starting inner rule
	finished inner rule
	finished middle rule
	finished outer rule

Die erste Regel (`outer rule`) umschließt also die mittlere (`middle rule`) und diese wiederum die dritte und letzte (`inner rule`).


## Schreib deine eigenen Regeln!

Warum sollte man Rules verwenden? Ein großer Pluspunkt von Rules ist ihre *Wiederverwendbarkeit*. Sie ermöglichen häufig benutzten Code, der bisher in `@Before`/`@After`-Methoden oder einer Testoberklasse stand, in eine eigene `TestRule`-Klasse auszulagern, die nur eine Verantwortlichkeit hat.

Ein weiterer Vorteil ist die *Kombinierbarkeit* von Rules. Wie wir in diesem Artikel gesehen haben, lassen sich beliebig viele Regeln in einem Test verwenden, sowohl auf Klassen- als auch auf Methodenebene. Viele Dinge, für die es in der Vergangenheit eines eigenen Test Runners bedurfte, lassen sich jetzt mit Rules implementieren. Da man immer nur einen Test Runner aber beliebig viele Rules verwenden kann, stehen einem deutlich mehr Möglichkeiten offen.

Rules sind die Umsetzung von *Delegation statt Vererbung* für Unittests. Wo früher Testklassenhierarchien mit Utility-Methoden gewuchert sind, kann man jetzt auf einfache Art und Weise verschiedene Rules kombinieren.

Die vorgestellten, konkreten Rules demonstrieren lediglich die Vielfältigkeit der Einsatzmöglichkeiten. Eigene Regeln zu schreiben ist Dank der zur Verfügung gestellten Templateklassen einfach. Erst diese *Erweiterbarkeit* macht Rules zu einem wirklichen Novum.

Die Macher von JUnit setzen jedenfalls für die Zukunft von JUnit voll auf den Einsatz und die Erweiterung von Rules. Kent Beck schreibt darüber in seinem Blog [[7][KentBeckBlog]]: „Maybe once every five years unsuspectedly powerful abstractions drop out of a program with no apparent effort.”


## Links & Literatur

1. [Kent Beck, Simple Smalltalk Testing: With Patterns][BeckSmalltalkTesting]
2. [Blog von Jens Schauder][JensSchauderBlog]
3. [Source Code der Beispiele auf GitHub][GitHubPage]
4. [JUnit 4.9 Release Notes][ReleaseNotes4.9]
5. [Mailing List Post von Kent Beck über das Design von Rules][KentBeckRuleChain]
6. [JUnit 4.10 Release Notes][ReleaseNotes4.10]
7. [Blog von Kent Beck][KentBeckBlog]


[BeckSmalltalkTesting]: http://www.xprogramming.com/testfram.htm "Kent Beck, Simple Smalltalk Testing: With Patterns"
[JensSchauderBlog]:     http://blog.schauderhaft.de/2009/10/04/junit-rules/ "Blog von Jens Schauder"
[GitHubPage]:           http://marcphilipp.github.com/junit-rules/ "Source Code der Beispiele auf GitHub"
[ReleaseNotes4.9]:      http://github.com/junit-team/junit/blob/master/doc/ReleaseNotes4.9.md "JUnit 4.9 Release Notes"
[KentBeckRuleChain]:    http://tech.groups.yahoo.com/group/junit/message/23537 "Mailing List Post von Kent Beck über das Design von Rules"
[ReleaseNotes4.10]:     https://github.com/junit-team/junit4/blob/master/doc/ReleaseNotes4.10.md "JUnit 4.10 Release Notes"
[KentBeckBlog]:         http://www.threeriversinstitute.org/blog/?p=155 "Blog von Kent Beck"
