package com.axway.maven.apigw;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 */
public abstract class AbstractGatewayMojo extends AbstractMojo {

	public static final String DIR_POLICIES = "policies";
	public static final String DIR_STATIC_FILES = "staticFiles";

	public static final String DEPENDENCY_FILE_NAME = ".projdeps.json";

	@Parameter(property = "axway.home", required = true)
	protected File homeAxway;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
	protected String finalName;

	@Parameter(property = "axway.dir.source", defaultValue = "${basedir}/src/main/axwgw", required = true)
	protected File sourceDirectory;

	@Parameter(property = "axway.dir.resources", defaultValue = "${basedir}/src/main/resources", required = true)
	protected File resourcesDirectory;

	@Parameter(property = "axway.dir.sharedProjects", defaultValue = "${project.build.directory}/sharedProjects", required = true)
	protected File sharedProjectsDir;

	@Parameter(property = "axway.dir.testServer", defaultValue = "${basedir}/src/test/policies")
	protected File testServerDirectory;

	@Parameter(property = "axway.home.apigw", defaultValue = "${axway.home}/apigateway", required = true)
	protected File homeAxwayGW;

	@Parameter(property = "axway.cmd.projpack", defaultValue = "${axway.home}/apigateway/Win32/bin/projpack.bat", required = true)
	protected File projpackPath;

	@Parameter(property = "axway.cmd.jython", defaultValue = "${axway.home}/apigateway/Win32/bin/jython.bat", required = true)
	protected File jythonCmd;

	@Parameter(property = "axway.cmd.policystudio", defaultValue = "${axway.home}/policystudio/policystudio.exe")
	protected File policyStudioCmd;

	@Parameter(property = "axway.cmd.configstudio", defaultValue = "${axway.home}/configurationstudio/configurationstudio.exe")
	protected File configStudioCmd;

	@Parameter(property = "axway.policystudio.data", defaultValue = "${basedir}/.data")
	protected File policyStudioDataDir;

	@Parameter(property = "axway.policystudio.dir", defaultValue = "${basedir}/.policystudio")
	protected File policyStudioDir;

	@Parameter(property = "axway.configstudio.data", defaultValue = "${basedir}/.configdata")
	protected File configStudioDataDir;

	@Parameter(property = "axway.configstudio.dir", defaultValue = "${basedir}/.configstudio")
	protected File configStudioDir;

	@Parameter(defaultValue = "${project}", readonly = true)
	protected MavenProject project;

	protected PackageType getPackageType() throws MojoExecutionException {
		String type = this.project.getArtifact().getType();
		try {
			return PackageType.fromType(type);
		} catch (IllegalArgumentException e) {
			throw new MojoExecutionException("Unsupported package type: " + type);
		}
	}
	
	protected File getTargetDir() {
		return new File(this.project.getBuild().getDirectory());
	}

	protected void checkAxwayHome() throws MojoExecutionException {
		if (!this.homeAxway.isDirectory() || !new File(this.homeAxway, "apigateway").isDirectory()) {
			throw new MojoExecutionException(
					"Directory '" + this.homeAxway.getPath() + "' is not a valid Axway home directory!");
		}
	}

	protected File getPoliciesDirectory() throws MojoExecutionException {
		return getPoliciesDirectory(this.sourceDirectory);
	}

	protected Optional<File> getStaticFilesDirectory() throws MojoExecutionException {
		return getStaticFilesDirectory(this.resourcesDirectory);
	}

	protected File getPoliciesDirectory(File srcDir) throws MojoExecutionException {
		File policiesDirectory = new File(srcDir, DIR_POLICIES);

		if (!policiesDirectory.exists()) {
			throw new MojoExecutionException("Invalid source directory layout: missing '" + DIR_POLICIES
					+ "' directory: " + policiesDirectory.getPath());
		}
		if (!policiesDirectory.isDirectory()) {
			throw new MojoExecutionException(
					"Invalid source directory layout: '" + policiesDirectory.getPath() + "' is not a directory!");
		}
		return policiesDirectory;
	}

	protected Optional<File> getStaticFilesDirectory(File srcDir) throws MojoExecutionException {
		File staticFilesDirectory = new File(srcDir, DIR_STATIC_FILES);

		if (!staticFilesDirectory.exists())
			return Optional.empty();

		if (!staticFilesDirectory.isDirectory())
			throw new MojoExecutionException(
					"Invalid static files directory: '" + staticFilesDirectory.getPath() + "' is not a directory!");

		return Optional.of(staticFilesDirectory);
	}

	protected List<Artifact> getDependentPolicyArchives() throws MojoExecutionException {
		PackageType pg = getPackageType();
		Set<String> includedTypes = new HashSet<String>();
		if (pg == PackageType.DEPLOYMENT) {
			includedTypes.add(PackageType.SERVER.getType());
		} else {
			includedTypes.add(PackageType.POLICY.getType());
		}
		return getDependencies(includedTypes);
	}

	protected List<Artifact> getDependentJars() throws MojoExecutionException {
		Set<String> includedTypes = new HashSet<String>();
		includedTypes.add("jar");
		return getDependencies(includedTypes);
	}

	protected List<Artifact> getDependencies(Set<String> includedTypes) {
		Set<Artifact> artifacts = this.project.getArtifacts();
		List<Artifact> deps = new ArrayList<Artifact>();

		for (Artifact a : artifacts) {
			if (includedTypes == null || includedTypes.contains(a.getType())) {
				getLog().info("Found dependency: " + a.getArtifactId());
				deps.add(a);
			}
		}

		return deps;
	}

	protected File getSharedArtifactDir(Artifact a) {
		return new File(this.sharedProjectsDir, a.getArtifactId());
	}
}
