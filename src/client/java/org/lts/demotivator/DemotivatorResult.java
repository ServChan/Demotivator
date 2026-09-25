package org.lts.demotivator;

import java.nio.file.Path;
import net.minecraft.resources.Identifier;

public record DemotivatorResult(Path file, int width, int height, Identifier texture) {
}
