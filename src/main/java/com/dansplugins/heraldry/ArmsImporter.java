package com.dansplugins.heraldry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Imports a coat of arms from a PNG in an import directory, such as {@code plugins/Heraldry/import},
 * and stores it as an owner's arms.
 *
 * <p>A player cannot upload a file through a vanilla client, so the image is placed in the import
 * directory by whoever runs the server, for example a PNG exported from Armoria. MVP.md restricts
 * issuing to operators or faction leaders rather than moderating submitted images, and this keeps
 * the choice of image with the same people.
 *
 * <p>Only a plain file name is accepted, never a path, so an import cannot read outside the
 * directory.
 */
public final class ArmsImporter {

    /** File names a player can type in a command: letters, digits, '.', '-' and '_', ending in .png. */
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}\\.png");

    private final Path directory;
    private final ArmsStore store;

    public ArmsImporter(Path directory, ArmsStore store) {
        if (directory == null || store == null) {
            throw new IllegalArgumentException("directory and store must not be null");
        }
        this.directory = directory;
        this.store = store;
    }

    /**
     * Reads {@code fileName} from the import directory and stores it as the owner's arms, replacing
     * any they already had.
     *
     * @return the imported image
     * @throws IllegalArgumentException if the name is not a plain {@code .png} file name, the file
     *                                  does not exist, or it is not a readable image. The message
     *                                  says which, and is suitable to show to the player.
     * @throws IOException              if the file cannot be read or the arms cannot be stored
     */
    public BufferedImage importArms(ArmsOwner owner, String fileName) throws IOException {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        Path file = resolve(fileName);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("No file named " + fileName + " in the import folder.");
        }
        BufferedImage arms;
        try (InputStream in = Files.newInputStream(file)) {
            arms = ImageIO.read(in);
        }
        if (arms == null) {
            throw new IllegalArgumentException(fileName + " is not a readable PNG.");
        }
        store.save(owner, arms);
        return arms;
    }

    /** Returns the names of the PNG files in the import directory, sorted. Empty if it does not exist. */
    public List<String> list() throws IOException {
        if (!Files.isDirectory(directory)) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (Files.isRegularFile(entry) && VALID_NAME.matcher(name).matches()) {
                    names.add(name);
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    private Path resolve(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("A file name is required.");
        }
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            throw new IllegalArgumentException("SVG arms are not supported yet; export them as a PNG instead.");
        }
        if (!VALID_NAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("The file name must be a plain .png name, such as arms.png, but was: "
                    + fileName);
        }
        return directory.resolve(fileName);
    }
}
