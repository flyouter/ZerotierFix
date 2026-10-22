package buildlogic;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.greenrobot.greendao.codemodifier.FormattingOptions;
import org.greenrobot.greendao.codemodifier.Greendao3Generator;
import org.greenrobot.greendao.codemodifier.SchemaOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs greenDAO's code generator against the annotated entities.
 *
 * <p>Replaces the legacy {@code org.greenrobot.greendao} Gradle plugin, which
 * cannot be applied on AGP 9 because it resolves the removed
 * {@code AndroidConfig} extension.</p>
 */
public abstract class GreendaoGenerateTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceFiles();

    @Input
    public abstract Property<Integer> getSchemaVersion();

    @Input
    public abstract Property<String> getDaoPackage();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() throws IOException {
        List<File> candidates = new ArrayList<>();
        for (File file : getSourceFiles().getFiles()) {
            String source = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (source.contains("org.greenrobot.greendao.annotation")) {
                candidates.add(file);
            }
        }
        candidates.sort(Comparator.comparing(File::getPath));

        File outputDir = getOutputDir().get().getAsFile();
        outputDir.mkdirs();

        SchemaOptions schema = new SchemaOptions(
                "default",
                getSchemaVersion().get(),
                getDaoPackage().get(),
                outputDir,
                null);

        Map<String, SchemaOptions> schemas = new LinkedHashMap<>();
        schemas.put("default", schema);

        new Greendao3Generator(new FormattingOptions(null, null), Collections.emptyList(), "UTF-8")
                .run(candidates, schemas);
    }
}
