package jig.erd;

import jig.erd.application.repository.Repository;
import jig.erd.infrastructure.DataBaseDefinitionLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

public class JigErd {
    Logger logger = Logger.getLogger(JigErd.class.getName());

    DataSource dataSource;

    public JigErd(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static void run(DataSource dataSource) {
        JigErd jigErd = new JigErd(dataSource);
        jigErd.run();
    }

    public void run() {
        Repository repository = new Repository();
        new DataBaseDefinitionLoader(dataSource, repository).load();

        exportDiagram(repository.columnRelationDiagram().dotText(), "jig-er-detail");
        exportDiagram(repository.entityRelationDiagram().dotText(), "jig-er-summary");
    }

    private void exportDiagram(String graphText, String diagramFileName) {
        try {
            Path dir = Paths.get("");
            Path gvPath = dir.resolve(diagramFileName + ".gv");
            Files.writeString(gvPath, graphText, StandardCharsets.UTF_8);
            logger.info("DOT file: " + gvPath.toAbsolutePath());

            Path imagePath = dir.resolve(diagramFileName + ".svg");

            String[] dotCommand = {"dot", "-Tsvg", "-o" + imagePath.toAbsolutePath(), gvPath.toAbsolutePath().toString()};
            logger.info("command: " + Arrays.toString(dotCommand));

            int code = new ProcessBuilder()
                    .command(dotCommand)
                    .start()
                    .waitFor();
            if (code == 0) {
                logger.info("image file: " + imagePath.toAbsolutePath());
                logger.info("delete DOT file.");
                Files.deleteIfExists(gvPath);
            } else {
                logger.warning("dot command failed: exit code: " + code);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}