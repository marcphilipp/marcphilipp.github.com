package de.marcphilipp.website;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.marcphilipp.speakerdeck.FileDownloader;
import de.marcphilipp.speakerdeck.JsoupFileDownloader;
import de.marcphilipp.speakerdeck.ScrapingSpeakerDeckApi;
import de.marcphilipp.speakerdeck.SpeakerDeckApi;
import de.marcphilipp.speakerdeck.SpeakerDeckApi.PresentationIdentifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

public class TalksPostProcessor {

    private final SpeakerDeckApi api = new ScrapingSpeakerDeckApi();
    private final FileDownloader fileDownloader = new JsoupFileDownloader();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(WRITE_DOC_START_MARKER).enable(MINIMIZE_QUOTES));

    public void process(Path talksYml, Path rootDir, Path imageDir, Path targetYamlFile) throws IOException {
        var arrayNode = (ArrayNode) mapper.readTree(talksYml.toFile());
        StreamSupport.stream(arrayNode.spliterator(), false)
                .filter(JsonNode::isObject)
                .map(ObjectNode.class::cast)
                .filter(node -> node.get("slides") != null)
                .parallel()
                .forEach(node -> {
                    var presentationUrl = URI.create(node.get("slides").textValue());
                    var presentationIdentifier = PresentationIdentifier.parse(presentationUrl);
                    var details = api.getPresentationDetails(presentationIdentifier);
                    try {
                        String fileName = Paths.get(details.imageUrl().getPath()).getFileName().toString();
                        String extension = fileName.substring(fileName.lastIndexOf('.'));

                        Path usernameDir = Files.createDirectories(imageDir.resolve(presentationIdentifier.user().userName()));
                        Path targetFile = usernameDir.resolve(presentationIdentifier.presentation() + extension);
                        fileDownloader.download(details.imageUrl(), targetFile);

                        node.set("slideImage", new TextNode(rootDir.relativize(targetFile).toString()));
                        node.set("thread", new TextNode(Thread.currentThread().getName()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        mapper.writeValue(targetYamlFile.toFile(), arrayNode);
    }
}
