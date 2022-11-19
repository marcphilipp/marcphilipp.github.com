package de.marcphilipp.speakerdeck;

import java.net.URI;

public interface SpeakerDeckApi {

    URI BASE_URI = URI.create("https://speakerdeck.com/");

    PresentationDetails getPresentationDetails(PresentationIdentifier identifier);

    record PresentationDetails(PresentationIdentifier identifier, String title, String description, URI imageUrl) {}

    record PresentationIdentifier(UserIdentifier user, String presentation) {
        public static PresentationIdentifier parse(URI url) {
            if (!BASE_URI.getHost().equals(url.getHost())) {
                throw new IllegalArgumentException("Wrong host: " + url);
            }
            return parse(url.getPath().substring(1));
        }
        public static PresentationIdentifier parse(String path) {
            String[] parts = path.split("/", 2);
            return new PresentationIdentifier(new UserIdentifier(parts[0]), parts[1]);
        }
    }

    record UserIdentifier(String userName) {}

}
