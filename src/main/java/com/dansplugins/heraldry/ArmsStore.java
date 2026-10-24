package com.dansplugins.heraldry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

/**
 * Persists each owner's coat of arms across restarts, one PNG per owner.
 *
 * <p>Files live at {@code <root>/<type>/<id>.png}, for example {@code arms/player/<uuid>.png}. The
 * imported image is stored rather than its rasterised map pixels, so it can be rasterised again
 * against whichever map palette the running server has.
 */
public final class ArmsStore {

    private final Path root;

    public ArmsStore(Path root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
    }

    /**
     * Stores an owner's arms, replacing any they already had. The file is written to a temporary
     * name first and then moved into place, so a crash mid-write leaves the previous arms intact.
     */
    public void save(ArmsOwner owner, BufferedImage arms) throws IOException {
        if (arms == null) {
            throw new IllegalArgumentException("arms must not be null");
        }
        Path file = fileFor(owner);
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        if (!ImageIO.write(arms, "png", temporary.toFile())) {
            throw new IOException("No PNG writer is available to store the arms of " + owner);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Returns the owner's arms, or empty if they have none. */
    public Optional<BufferedImage> load(ArmsOwner owner) throws IOException {
        Path file = fileFor(owner);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        BufferedImage arms;
        try (InputStream in = Files.newInputStream(file)) {
            arms = ImageIO.read(in);
        }
        if (arms == null) {
            throw new IOException("Stored arms of " + owner + " are not a readable image: " + file);
        }
        return Optional.of(arms);
    }

    public boolean has(ArmsOwner owner) {
        return Files.isRegularFile(fileFor(owner));
    }

    /** Removes the owner's arms. Returns whether they had any. */
    public boolean delete(ArmsOwner owner) throws IOException {
        return Files.deleteIfExists(fileFor(owner));
    }

    Path fileFor(ArmsOwner owner) {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        return root.resolve(owner.getType().name().toLowerCase(Locale.ROOT)).resolve(owner.getId() + ".png");
    }
}
