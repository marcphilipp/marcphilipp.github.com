package de.marcphilipp.speakerdeck;

import java.net.URI;
import java.nio.file.Path;

public interface FileDownloader {

    void download(URI url, Path targetDir);

}
