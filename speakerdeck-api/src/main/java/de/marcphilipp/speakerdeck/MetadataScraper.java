package de.marcphilipp.speakerdeck;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Optional;

public class MetadataScraper {

    public static Optional<URI> getImage(URI url) {
        var head = get(url).head();
        return Optional.ofNullable(head.select("meta[property=og:image]").first()).map(it -> url.resolve(it.attr("content")));
    }

    private static Document get(URI url) {
        try {
            return Jsoup.connect(url.toString()).get();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to download " + url, e);
        }
    }

}
