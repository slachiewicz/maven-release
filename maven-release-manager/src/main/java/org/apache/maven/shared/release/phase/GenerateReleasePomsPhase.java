/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.release.phase;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Extension;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.model.Resource;
import org.apache.maven.api.model.Scm;
import org.apache.maven.api.services.SuperPomProvider;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmCommandException;
import org.apache.maven.shared.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.shared.release.scm.ScmTranslator;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.apache.maven.shared.release.versions.ArtifactKeys;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

import static java.util.Objects.requireNonNull;

/**
 * Generate release POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
@Singleton
@Named("generate-release-poms")
public class GenerateReleasePomsPhase extends AbstractReleasePomsPhase implements ResourceGenerator {
    private static final String FINALNAME_EXPRESSION = "${project.artifactId}-${project.version}";

    private final SuperPomProvider superPomProvider;

    private final ModelXmlFactory modelXmlFactory;

    /**
     * SCM URL translators mapped by provider name.
     */
    private final Map<String, ScmTranslator> scmTranslators;

    @Inject
    public GenerateReleasePomsPhase(
            ScmRepositoryConfigurator scmRepositoryConfigurator,
            SuperPomProvider superPomProvider,
            ModelXmlFactory modelXmlFactory,
            Map<String, ScmTranslator> scmTranslators) {
        super(scmRepositoryConfigurator);
        this.superPomProvider = requireNonNull(superPomProvider);
        this.modelXmlFactory = requireNonNull(modelXmlFactory);
        this.scmTranslators = requireNonNull(scmTranslators);
    }

    /*
     * @see org.apache.maven.shared.release.phase.ReleasePhase#execute(org.apache.maven.shared.release.config.ReleaseDescriptor,
     *      org.apache.maven.shared.release.env.ReleaseEnvironment, java.util.List)
     */
    @Override
    public ReleaseResult execute(
            ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<Project> reactorProjects)
            throws ReleaseExecutionException, ReleaseFailureException {
        return execute(releaseDescriptor, releaseEnvironment, reactorProjects, false);
    }

    private ReleaseResult execute(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<Project> reactorProjects,
            boolean simulate)
            throws ReleaseExecutionException, ReleaseFailureException {
        ReleaseResult result = new ReleaseResult();

        if (releaseDescriptor.isGenerateReleasePoms()) {
            logInfo(result, "Generating release POMs...");

            generateReleasePoms(releaseDescriptor, releaseEnvironment, reactorProjects, simulate, result);
        } else {
            logInfo(result, "Not generating release POMs");
        }

        result.setResultCode(ReleaseResult.SUCCESS);

        return result;
    }

    private void generateReleasePoms(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<Project> reactorProjects,
            boolean simulate,
            ReleaseResult result)
            throws ReleaseExecutionException, ReleaseFailureException {
        List<File> releasePoms = new ArrayList<>();

        for (Project project : reactorProjects) {
            logInfo(result, "Generating release POM for '" + ReleaseUtil.getProjectName(project) + "'...");

            releasePoms.add(generateReleasePom(project, releaseDescriptor, releaseEnvironment, result));
        }

        addReleasePomsToScm(releaseDescriptor, releaseEnvironment, reactorProjects, simulate, result, releasePoms);
    }

    private File generateReleasePom(
            Project project,
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            ReleaseResult result)
            throws ReleaseExecutionException, ReleaseFailureException {
        // create release pom

        Model releasePom = createReleaseModel(project, releaseDescriptor, releaseEnvironment, result);

        // write release pom to file

        File releasePomFile = ReleaseUtil.getReleasePom(project);

        // MRELEASE-273 : A release pom can be null
        if (releasePomFile == null) {
            throw new ReleaseExecutionException("Cannot generate release POM : pom file is null");
        }

        try {
            modelXmlFactory.write(releasePom, releasePomFile.toPath());
        } catch (XmlWriterException exception) {
            throw new ReleaseExecutionException("Cannot generate release POM", exception);
        }

        return releasePomFile;
    }

    private void addReleasePomsToScm(
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            List<Project> reactorProjects,
            boolean simulate,
            ReleaseResult result,
            List<File> releasePoms)
            throws ReleaseFailureException, ReleaseExecutionException {
        if (simulate) {
            logInfo(result, "Full run would be adding " + releasePoms);
        } else {
            ScmRepository scmRepository = getScmRepository(releaseDescriptor, releaseEnvironment);
            ScmProvider scmProvider = getScmProvider(scmRepository);

            Project rootProject = ReleaseUtil.getRootProject(reactorProjects);
            ScmFileSet scmFileSet =
                    new ScmFileSet(ReleaseUtil.getPomFile(rootProject).getParentFile(), releasePoms);

            try {
                AddScmResult scmResult = scmProvider.add(scmRepository, scmFileSet);

                if (!scmResult.isSuccess()) {
                    throw new ReleaseScmCommandException("Cannot add release POM to SCM", scmResult);
                }
            } catch (ScmException exception) {
                throw new ReleaseExecutionException(
                        "Cannot add release POM to SCM: " + exception.getMessage(), exception);
            }
        }
    }

    private Model createReleaseModel(
            Project project,
            ReleaseDescriptor releaseDescriptor,
            ReleaseEnvironment releaseEnvironment,
            ReleaseResult result)
            throws ReleaseFailureException, ReleaseExecutionException {
        Model effectiveModel = project.getModel();
        ProjectArtifacts projectArtifacts = ProjectArtifacts.of(releaseEnvironment.getSession(), project);

        // the release POM should reflect bits of these which were injected at build time...
        // we don't need these polluting the POM.
        // a builder from with() ignores null values, so these are cleared with newBuilder(model, true)
        Model.Builder releaseBuilder = Model.newBuilder(effectiveModel, true)
                .parent(null)
                .profiles(Collections.emptyList())
                .dependencyManagement(null);
        Build.Builder buildBuilder =
                Build.newBuilder(effectiveModel.getBuild(), true).pluginManagement(null);

        // update project version
        String releaseVersion = getNextVersion(releaseDescriptor, project.getGroupId(), project.getArtifactId());
        releaseBuilder.version(releaseVersion);

        // interpolation of the final name needs the release version
        Model releaseModel = releaseBuilder.build();

        String originalFinalName = effectiveModel.getBuild().getFinalName();
        // update final name if implicit
        if (!FINALNAME_EXPRESSION.equals(originalFinalName)) {
            originalFinalName = findOriginalFinalName(project);

            if (originalFinalName == null) {
                // as defined in super-pom
                originalFinalName = FINALNAME_EXPRESSION;
            }
        }

        // make finalName always explicit
        String finalName = ReleaseUtil.interpolate(originalFinalName, releaseModel);

        // still required?
        if (finalName.contains(ArtifactKeys.SNAPSHOT_VERSION)) {
            throw new ReleaseFailureException("Cannot reliably adjust the finalName of project: " + project.getId());
        }
        buildBuilder.finalName(finalName);

        // update scm
        Scm scm = effectiveModel.getScm();

        if (scm != null) {
            ScmRepository scmRepository = getScmRepository(releaseDescriptor, releaseEnvironment);
            ScmTranslator scmTranslator = getScmTranslator(scmRepository);

            if (scmTranslator != null) {
                releaseBuilder.scm(createReleaseScm(scm, scmTranslator, releaseDescriptor));
            } else {
                String message = "No SCM translator found - skipping rewrite";

                result.appendDebug(message);

                getLogger().debug(message);
            }
        }

        // rewrite dependencies
        releaseBuilder.dependencies(createReleaseDependencies(releaseDescriptor, projectArtifacts));

        // rewrite plugins
        buildBuilder.plugins(createReleasePlugins(releaseDescriptor, project, projectArtifacts));

        // rewrite reports
        Reporting.Builder reportingBuilder = effectiveModel.getReporting() != null
                ? effectiveModel.getReporting().with()
                : Reporting.newBuilder();
        reportingBuilder.plugins(createReleaseReportPlugins(releaseDescriptor, project, projectArtifacts));

        // rewrite extensions
        buildBuilder.extensions(createReleaseExtensions(releaseDescriptor, project, projectArtifacts));

        releaseBuilder.build(buildBuilder.build()).reporting(reportingBuilder.build());

        return unalignFromBaseDirectory(releaseBuilder.build(), ReleaseUtil.getBasedir(project));
    }

    private Model unalignFromBaseDirectory(Model releaseModel, File basedir) throws ReleaseExecutionException {
        Model rawSuperModel = superPomProvider.getSuperPom(releaseModel.getModelVersion());

        Path basePath = basedir.toPath();
        Build currentBuild = releaseModel.getBuild();
        Build rawSuperBuild = rawSuperModel.getBuild();

        // the super POM values, once with expressions replaced ("interpolated") and once as declared ("raw")
        Build.Builder buildBuilder = currentBuild.with();
        buildBuilder.sourceDirectory(resolvePath(
                basePath,
                currentBuild.getSourceDirectory(),
                superValue(rawSuperModel, rawSuperBuild.getSourceDirectory(), basedir, releaseModel),
                rawSuperBuild.getSourceDirectory()));
        buildBuilder.scriptSourceDirectory(resolvePath(
                basePath,
                currentBuild.getScriptSourceDirectory(),
                superValue(rawSuperModel, rawSuperBuild.getScriptSourceDirectory(), basedir, releaseModel),
                rawSuperBuild.getScriptSourceDirectory()));
        buildBuilder.testSourceDirectory(resolvePath(
                basePath,
                currentBuild.getTestSourceDirectory(),
                superValue(rawSuperModel, rawSuperBuild.getTestSourceDirectory(), basedir, releaseModel),
                rawSuperBuild.getTestSourceDirectory()));
        buildBuilder.outputDirectory(resolvePath(
                basePath,
                currentBuild.getOutputDirectory(),
                superValue(rawSuperModel, rawSuperBuild.getOutputDirectory(), basedir, releaseModel),
                rawSuperBuild.getOutputDirectory()));
        buildBuilder.testOutputDirectory(resolvePath(
                basePath,
                currentBuild.getTestOutputDirectory(),
                superValue(rawSuperModel, rawSuperBuild.getTestOutputDirectory(), basedir, releaseModel),
                rawSuperBuild.getTestOutputDirectory()));
        buildBuilder.directory(resolvePath(
                basePath,
                currentBuild.getDirectory(),
                superValue(rawSuperModel, rawSuperBuild.getDirectory(), basedir, releaseModel),
                rawSuperBuild.getDirectory()));

        buildBuilder.resources(unalignResources(
                basePath,
                currentBuild.getResources(),
                rawSuperBuild.getResources(),
                rawSuperModel,
                basedir,
                releaseModel));
        buildBuilder.testResources(unalignResources(
                basePath,
                currentBuild.getTestResources(),
                rawSuperBuild.getTestResources(),
                rawSuperModel,
                basedir,
                releaseModel));

        Reporting reporting = releaseModel.getReporting();
        Reporting rawSuperReporting = rawSuperModel.getReporting();
        Reporting.Builder reportingBuilder = reporting.with();
        reportingBuilder.outputDirectory(resolvePath(
                basePath,
                reporting.getOutputDirectory(),
                superValue(rawSuperModel, rawSuperReporting.getOutputDirectory(), basedir, releaseModel),
                rawSuperReporting.getOutputDirectory()));

        return releaseModel
                .with()
                .build(buildBuilder.build())
                .reporting(reportingBuilder.build())
                .build();
    }

    private List<Resource> unalignResources(
            Path basedir,
            List<Resource> currentResources,
            List<Resource> rawSuperResources,
            Model rawSuperModel,
            File basedirFile,
            Model releaseModel)
            throws ReleaseExecutionException {
        Map<String, String> superResourceDirectories = new LinkedHashMap<>(rawSuperResources.size());
        for (Resource rawSuperResource : rawSuperResources) {
            superResourceDirectories.put(
                    superValue(rawSuperModel, rawSuperResource.getDirectory(), basedirFile, releaseModel),
                    rawSuperResource.getDirectory());
        }

        List<Resource> resources = new ArrayList<>(currentResources.size());
        for (Resource currentResource : currentResources) {
            resources.add(currentResource
                    .with()
                    .directory(resolvePath(basedir, currentResource.getDirectory(), superResourceDirectories))
                    .build());
        }
        return resources;
    }

    /**
     * Replaces the expressions of a super POM value the way the Maven 3 model interpolator did: {@code basedir},
     * the version and artifact id of the release model, and everything else by reading the super POM itself.
     */
    private String superValue(Model rawSuperModel, String value, File basedir, Model releaseModel)
            throws ReleaseExecutionException {
        if (value == null || !value.contains("${")) {
            return value;
        }
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        List<String> pomPrefixes = java.util.Arrays.asList("pom.", "project.");
        Map<String, String> known = new LinkedHashMap<>();
        // inject proper values used by project.build.finalName
        known.put("project.version", releaseModel.getVersion());
        known.put("project.artifactId", releaseModel.getArtifactId());
        known.put("project.basedir", basedir.getAbsolutePath());
        known.put("pom.basedir", basedir.getAbsolutePath());
        known.put("basedir", basedir.getAbsolutePath());
        interpolator.addValueSource(new MapBasedValueSource(known));
        interpolator.addValueSource(new PrefixedObjectValueSource(pomPrefixes, rawSuperModel, false));
        try {
            return interpolator.interpolate(value, new PrefixAwareRecursionInterceptor(pomPrefixes));
        } catch (InterpolationException e) {
            throw new ReleaseExecutionException("Failed to interpolate " + value + " of the super POM", e);
        }
    }

    private String resolvePath(Path basedir, String current, String superInterpolated, String superRaw) {
        return basedir.resolve(current).equals(basedir.resolve(superInterpolated)) ? superRaw : current;
    }

    private String resolvePath(
            Path basedir, String current, Map<String /* interpolated */, String /* raw */> superValues) {
        for (Map.Entry<String, String> superValue : superValues.entrySet()) {
            if (basedir.resolve(current).equals(basedir.resolve(superValue.getKey()))) {
                return superValue.getValue();
            }
        }
        return current;
    }

    private String findOriginalFinalName(Project project) throws ReleaseExecutionException {
        Model originalModel = readOriginalModel(project);
        if (originalModel.getBuild() != null && originalModel.getBuild().getFinalName() != null) {
            return originalModel.getBuild().getFinalName();
        } else if (project.getParent().isPresent()) {
            return findOriginalFinalName(project.getParent().get());
        } else {
            return null;
        }
    }

    private Model readOriginalModel(Project project) throws ReleaseExecutionException {
        try {
            return modelXmlFactory.read(project.getPomPath(), false);
        } catch (XmlReaderException e) {
            throw new ReleaseExecutionException("Cannot read the POM of " + project.getId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ReleaseResult simulate(
            ReleaseDescriptor releaseDescriptor, ReleaseEnvironment releaseEnvironment, List<Project> reactorProjects)
            throws ReleaseExecutionException, ReleaseFailureException {
        return execute(releaseDescriptor, releaseEnvironment, reactorProjects, true);
    }

    private String getNextVersion(ReleaseDescriptor releaseDescriptor, String groupId, String artifactId)
            throws ReleaseFailureException {
        // TODO: share with RewritePomsForReleasePhase.rewriteVersion

        String id = ArtifactKeys.versionlessKey(groupId, artifactId);

        String nextVersion = releaseDescriptor.getProjectReleaseVersion(id);

        if (nextVersion == null) {
            throw new ReleaseFailureException("Version for '" + id + "' was not mapped");
        }

        return nextVersion;
    }

    private ScmTranslator getScmTranslator(ScmRepository scmRepository) {
        return scmTranslators.get(scmRepository.getProvider());
    }

    private Scm createReleaseScm(Scm scm, ScmTranslator scmTranslator, ReleaseDescriptor releaseDescriptor) {
        // TODO: share with RewritePomsForReleasePhase.translateScm

        String tag = releaseDescriptor.getScmReleaseLabel();
        String tagBase = releaseDescriptor.getScmTagBase();

        Scm.Builder releaseScm = Scm.newBuilder();

        if (scm.getConnection() != null) {
            String value = scmTranslator.translateTagUrl(scm.getConnection(), tag, tagBase);
            releaseScm.connection(value);
        }

        if (scm.getDeveloperConnection() != null) {
            String value = scmTranslator.translateTagUrl(scm.getDeveloperConnection(), tag, tagBase);
            releaseScm.developerConnection(value);
        }

        if (scm.getUrl() != null) {
            String value = scmTranslator.translateTagUrl(scm.getUrl(), tag, tagBase);
            releaseScm.url(value);
        }

        if (scm.getTag() != null) {
            String value = scmTranslator.resolveTag(scm.getTag());
            releaseScm.tag(value);
        }

        return releaseScm.build();
    }

    private List<Dependency> createReleaseDependencies(
            ReleaseDescriptor releaseDescriptor, ProjectArtifacts projectArtifacts) throws ReleaseFailureException {
        // make dependency order deterministic for tests (related to MNG-1412)
        List<ResolvedArtifact> orderedArtifacts = new ArrayList<>(projectArtifacts.getDependencyArtifacts());
        Collections.sort(orderedArtifacts);

        List<Dependency> releaseDependencies = new ArrayList<>();

        for (ResolvedArtifact artifact : orderedArtifacts) {
            String version = getReleaseVersion(releaseDescriptor, artifact);

            releaseDependencies.add(Dependency.newBuilder()
                    .groupId(artifact.getGroupId())
                    .artifactId(artifact.getArtifactId())
                    .version(version)
                    .type(artifact.getType())
                    .scope(artifact.getScope())
                    .classifier(artifact.getClassifier())
                    .build());
        }

        return releaseDependencies;
    }

    private String getReleaseVersion(ReleaseDescriptor releaseDescriptor, ResolvedArtifact artifact)
            throws ReleaseFailureException {
        String key = artifact.versionlessKey();

        String originalVersion = releaseDescriptor.getProjectOriginalVersion(key);
        String mappedVersion = releaseDescriptor.getProjectReleaseVersion(key);

        String version = artifact.getVersion();

        if (version.equals(originalVersion)) {
            if (mappedVersion != null) {
                version = mappedVersion;
            } else {
                throw new ReleaseFailureException("Version '" + version + "' for '" + key + "' was not mapped");
            }
        } else {
            if (!ArtifactKeys.isSnapshot(version)) {
                version = artifact.getBaseVersion();
            }
        }

        return version;
    }

    /**
     * Pins the version of a plugin, report plugin or extension. Maven 3 resolved unversioned ones to the latest
     * release; the Maven 4 API cannot do that, so they stay unversioned.
     */
    private String getReleaseVersion(
            ReleaseDescriptor releaseDescriptor,
            Map<String, ResolvedArtifact> artifactsById,
            String groupId,
            String artifactId)
            throws ReleaseFailureException {
        ResolvedArtifact artifact = artifactsById.get(ArtifactKeys.versionlessKey(groupId, artifactId));
        return artifact == null ? null : getReleaseVersion(releaseDescriptor, artifact);
    }

    private List<Plugin> createReleasePlugins(
            ReleaseDescriptor releaseDescriptor, Project project, ProjectArtifacts projectArtifacts)
            throws ReleaseFailureException, ReleaseExecutionException {
        List<Plugin> releasePlugins = new ArrayList<>();

        // Use original - don't want the lifecycle introduced ones
        Build build = readOriginalModel(project).getBuild();

        if (build != null) {
            for (Plugin plugin : build.getPlugins()) {
                String version = getReleaseVersion(
                        releaseDescriptor,
                        projectArtifacts.getPluginArtifactMap(),
                        plugin.getGroupId(),
                        plugin.getArtifactId());

                releasePlugins.add(Plugin.newBuilder()
                        .groupId(plugin.getGroupId())
                        .artifactId(plugin.getArtifactId())
                        .version(version)
                        .extensions(plugin.getExtensions())
                        .executions(plugin.getExecutions())
                        .dependencies(plugin.getDependencies())
                        .inherited(plugin.getInherited())
                        .configuration(plugin.getConfiguration())
                        .build());
            }
        }

        return releasePlugins;
    }

    private List<ReportPlugin> createReleaseReportPlugins(
            ReleaseDescriptor releaseDescriptor, Project project, ProjectArtifacts projectArtifacts)
            throws ReleaseFailureException {
        List<ReportPlugin> releaseReportPlugins = new ArrayList<>();

        Reporting reporting = project.getModel().getReporting();

        if (reporting != null) {
            for (ReportPlugin reportPlugin : reporting.getPlugins()) {
                String version = getReleaseVersion(
                        releaseDescriptor,
                        projectArtifacts.getReportArtifactMap(),
                        reportPlugin.getGroupId(),
                        reportPlugin.getArtifactId());

                releaseReportPlugins.add(ReportPlugin.newBuilder()
                        .groupId(reportPlugin.getGroupId())
                        .artifactId(reportPlugin.getArtifactId())
                        .version(version)
                        .inherited(reportPlugin.getInherited())
                        .configuration(reportPlugin.getConfiguration())
                        .reportSets(reportPlugin.getReportSets())
                        .build());
            }
        }

        return releaseReportPlugins;
    }

    private List<Extension> createReleaseExtensions(
            ReleaseDescriptor releaseDescriptor, Project project, ProjectArtifacts projectArtifacts)
            throws ReleaseFailureException, ReleaseExecutionException {
        List<Extension> releaseExtensions = new ArrayList<>();

        // Use original - don't want the lifecycle introduced ones
        Build build = readOriginalModel(project).getBuild();

        if (build != null) {
            for (Extension extension : build.getExtensions()) {
                String version = getReleaseVersion(
                        releaseDescriptor,
                        projectArtifacts.getExtensionArtifactMap(),
                        extension.getGroupId(),
                        extension.getArtifactId());

                releaseExtensions.add(Extension.newBuilder()
                        .groupId(extension.getGroupId())
                        .artifactId(extension.getArtifactId())
                        .version(version)
                        .build());
            }
        }

        return releaseExtensions;
    }

    /*
     * @see org.apache.maven.shared.release.phase.AbstractReleasePhase#clean(java.util.List)
     */
    @Override
    public ReleaseResult clean(List<Project> reactorProjects) {
        ReleaseResult result = new ReleaseResult();

        for (Project project : reactorProjects) {
            File releasePom = ReleaseUtil.getReleasePom(project);

            // MRELEASE-273 : A release pom can be null
            if (releasePom != null && releasePom.exists()) {
                logInfo(result, "Deleting release POM for '" + ReleaseUtil.getProjectName(project) + "'...");

                if (!releasePom.delete()) {
                    logWarn(result, "Cannot delete release POM: " + releasePom);
                }
            }
        }

        result.setResultCode(ReleaseResult.SUCCESS);

        return result;
    }
}
