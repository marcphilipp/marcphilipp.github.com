package de.marcphilipp.speakerdeck;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import static java.util.Objects.requireNonNull;

public class ScrapingSpeakerDeckApi implements SpeakerDeckApi {

    @Override
    public PresentationDetails getPresentationDetails(PresentationIdentifier identifier) {
        Document document = get(BASE_URI.resolve(identifier.user().userName() + "/").resolve(identifier.presentation()));
        var title = requireNonNull(document.head().select("meta[property=og:title]").first()).attr("content");
        var description = requireNonNull(document.head().select("meta[property=og:description]").first()).attr("content");
        var imageUrl = URI.create(requireNonNull(document.head().select("meta[property=og:image]").first()).attr("content"));
        return new PresentationDetails(identifier, title, description, imageUrl);
    }

    private static Document get(URI url) {
        try {
            return Jsoup.connect(url.toString()).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
