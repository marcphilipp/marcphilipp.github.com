package de.marcphilipp.speakerdeck;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class JsoupFileDownloader implements FileDownloader {

    @Override
    public void download(URI url, Path targetFile) {
        try {
            var response = Jsoup.connect(url.toString())
                    .ignoreContentType(true)
                    .execute();
            try (InputStream in = response.bodyStream()) {
                Files.copy(in, targetFile, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
