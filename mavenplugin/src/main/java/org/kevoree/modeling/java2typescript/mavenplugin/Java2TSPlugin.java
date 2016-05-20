package org.kevoree.modeling.java2typescript.mavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.kevoree.modeling.java2typescript.FlatJUnitGenerator;
import org.kevoree.modeling.java2typescript.SourceTranslator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "java2ts", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class Java2TSPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    protected File source;

    @Parameter()
    protected List<File> sources = new ArrayList<>();

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/java2ts")
    protected File target;

    @Parameter(defaultValue = "${project.artifactId}")
    private String name;

    @Parameter(defaultValue = "${project.artifactId}")
    private String packageName;

    @Parameter(defaultValue = "${project.version}")
    private String packageVersion;

    @Parameter
    private List<Dependency> dependencies = new ArrayList<Dependency>();

    @Parameter
    private List<String> moduleImports = new ArrayList<String>();

    @Parameter
    private Map<String, String> pkgTransforms = new HashMap<String, String>();

    @Parameter(defaultValue = "false")
    private boolean copyJRE;

    @Parameter(defaultValue = "false")
    private boolean copyJunit;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<String> sources = new ArrayList<>();
        if (sources.isEmpty()) {
            sources.add(source.getAbsolutePath());
        } else {
            sources.addAll(this.sources.stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        }

        SourceTranslator sourceTranslator = new SourceTranslator(sources, target.getPath(), name);
        moduleImports.forEach(sourceTranslator::addModuleImport);
        if(copyJRE) {
            sourceTranslator.addModuleImport("./jre.ts");
        }
        if(copyJunit) {
            sourceTranslator.addModuleImport("./junit.ts");
        }
        pkgTransforms.forEach(sourceTranslator::addPackageTransform);


        for (Artifact a : project.getDependencyArtifacts()) {
            File file = a.getFile();
            if (file != null) {
                if (file.isFile()) {
                    sourceTranslator.addToClasspath(file.getAbsolutePath());
                    getLog().info(file.getAbsolutePath() + " added to Java2TS analyzer");
                }
            }
        }

        sourceTranslator.process();

        sourceTranslator.generate();
        if (copyJRE) {
            try {
                Files.copy(getClass().getClassLoader().getResourceAsStream("java.ts"), Paths.get(target.getAbsolutePath(),"src","main","jre.ts"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (copyJunit) {
            try {
                Files.copy(getClass().getClassLoader().getResourceAsStream("junit.ts"), Paths.get(target.getAbsolutePath(),"src","main","junit.ts"), StandardCopyOption.REPLACE_EXISTING);
                FlatJUnitGenerator generator = new FlatJUnitGenerator();
                generator.addModuleImport("./" + name + ".ts");
                generator.generate(source, Paths.get(target.getAbsolutePath(),"src","main").toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}