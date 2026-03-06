package de.marcphilipp.website;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.marcphilipp.speakerdeck.FileDownloader;
import de.marcphilipp.speakerdeck.JsoupFileDownloader;
import de.marcphilipp.speakerdeck.MetadataScraper;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

@Command
public class TalksPostProcessor {

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
                    MetadataScraper.getImage(presentationUrl)
                            .ifPresent(imageUrl -> downloadImageAndUpdateMetadata(rootDir, imageDir, node, imageUrl, presentationUrl));
                });

        mapper.writeValue(targetYamlFile.toFile(), arrayNode);
    }

    private void downloadImageAndUpdateMetadata(Path rootDir, Path imageDir, ObjectNode node, URI imageUrl, URI presentationUrl) {
        try {
            var extension = getExtensionIncludingDot(imageUrl);
            var fileName = presentationUrl.getPath().substring(1);
            if (fileName.endsWith("/")) {
                fileName = fileName.substring(0, fileName.length() - 1);
            }
            fileName += extension;

            Path targetFile = imageDir.resolve(fileName);
            Files.createDirectories(targetFile.getParent());
            fileDownloader.download(imageUrl, targetFile);

            node.set("slideImage", new TextNode(rootDir.relativize(targetFile).toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getExtensionIncludingDot(URI imageUrl) {
        String fileName = Paths.get(imageUrl.getPath()).getFileName().toString();
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
