package de.marcphilipp.website;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(mixinStandardHelpOptions = true)
public class TalksPostProcessorMain implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new TalksPostProcessorMain()).execute(args);
        System.exit(exitCode);
    }

    @Option(names = "--input-yml")
    Path talksYml;

    @Option(names = "--site-dir")
    Path rootDir;

    @Option(names = "--image-dir")
    Path imageDir;

    @Option(names = "--output-yml")
    Path targetYamlFile;

    @Override
    public Integer call() throws Exception {
        new TalksPostProcessor().process(talksYml, rootDir, imageDir, targetYamlFile);
        return 0;
    }

}
