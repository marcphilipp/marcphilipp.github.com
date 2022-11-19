package de.marcphilipp.speakerdeck;

import de.marcphilipp.speakerdeck.SpeakerDeckApi.PresentationIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScrapingSpeakerDeckApiTests {

    SpeakerDeckApi api = new ScrapingSpeakerDeckApi();

    @Test
    void scrapesPresentationPage(@TempDir Path tempDir) {
        var details = api.getPresentationDetails(PresentationIdentifier.parse("marcphilipp/evolving-junit-5"));
        assertEquals("Evolving JUnit 5", details.title());
        var description = "Almost five years have passed since the initial release of JUnit 5 in 2017. But the JUnit team hasn’t ceased working since then. On the contrary, there have been nine additional 5.x releases. In this session, we’ll take a closer look at the latest new features, such as declarative test suites, custom JFR events, new extension points, improved support for temporary directories, the test method/class execution order, and the new XML reporting format. Of course, there will be time for questions from the audience as well. You should be able to learn something new in this session regardless of whether you’re a JUnit 5 novice or already have prior experience.";
        assertEquals(description, details.description());
        var imageFile = tempDir.resolve("title.jpg");
        new JsoupFileDownloader().download(details.imageUrl(), imageFile);
        assertTrue(Files.exists(imageFile));
    }
}
